/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.MockScheduledExecutorService;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.testUtil.NetworkTestUtil;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.ExecutorServiceUtil;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractSocketAppender}.
 *
 * @author Carl Harris
 * @author Sebastian Gr&ouml;bler
 */
public class AbstractSocketAppenderTest {

  /**
   * Timeout used for all blocking operations in multi-threading contexts.
   * Kept generous so the background connect/dispatch thread has ample time to
   * run under CI, where Mockito's inline mock maker (default since Mockito 5)
   * adds per-invocation instrumentation overhead. This is an upper bound for
   * verify(...) polling, not a fixed sleep, so passing tests stay fast.
   */
  private static final int TIMEOUT = 5000;

  /**
   * A malformed IPv6 literal: {@link InetAddress#getByName(String)} rejects it
   * with an {@link java.net.UnknownHostException} without any network lookup.
   */
  private static final String UNRESOLVABLE_HOST = "[not-an-ipv6-literal";

  private ScheduledExecutorService executorService;
  private MockContext mockContext;
  private PreSerializationTransformer<String> preSerializationTransformer;
  private Socket socket;
  private SocketConnector socketConnector;
  private AutoFlushingObjectWriter objectWriter;
  private ObjectWriterFactory objectWriterFactory;
  private LinkedBlockingDeque<String> deque;
  private QueueFactory queueFactory;
  private InstrumentedSocketAppender appender;
  private InstrumentedSocketAppender inlineAppender;

  @Before
  public void setupValidAppenderWithMockDependencies() throws Exception {
    // Use the real executor directly rather than a Mockito spy. The spy is
    // never verified, and wrapping a live thread pool in Mockito's inline mock
    // maker (default since Mockito 5) can interfere with its worker threads so
    // that submitted connect tasks intermittently never run, causing flaky
    // "zero interactions" failures (e.g. closesSocketOnException).
    executorService = ExecutorServiceUtil.newScheduledExecutorService();
    mockContext = new MockContext(executorService);
    preSerializationTransformer = spy(new StringPreSerializationTransformer());
    socket = mock(Socket.class);
    socketConnector = mock(SocketConnector.class);
    objectWriter = mock(AutoFlushingObjectWriter.class);
    objectWriterFactory = mock(ObjectWriterFactory.class);
    deque = spy(new LinkedBlockingDeque<String>(1));
    queueFactory = mock(QueueFactory.class);
    appender = spy(new InstrumentedSocketAppender(preSerializationTransformer, queueFactory, objectWriterFactory, socketConnector));

    doReturn(mock(OutputStream.class)).when(socket).getOutputStream();
    doReturn(objectWriter).when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));
    doReturn(deque).when(queueFactory).<String>newLinkedBlockingDeque(anyInt());

    appender.setContext(mockContext);
    appender.setRemoteHost("localhost");
  }

  @After
  public void tearDown() throws Exception {
    appender.stop();
    assertFalse(appender.isStarted());
    if (inlineAppender != null) {
      inlineAppender.stop();
    }

    executorService.shutdownNow();
    assertTrue(executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS));
  }

  @Test
  public void failsToStartWithoutValidPort() throws Exception {

    // given
    appender.setPort(-1);

    // when
    appender.start();

    // then
    assertFalse(appender.isStarted());
    verify(appender).addError(contains("port"));
  }

  @Test
  public void failsToStartWithoutValidRemoteHost() throws Exception {

    // given
    appender.setRemoteHost(null);

    // when
    appender.start();

    // then
    assertFalse(appender.isStarted());
    verify(appender).addError(contains("remote host"));
  }

  @Test
  public void failsToStartWithNegativeQueueSize() throws Exception {

    // given
    appender.setQueueSize(-1);

    // when
    appender.start();

    // then
    assertFalse(appender.isStarted());
    verify(appender).addError(contains("Queue size must be greater than zero"));
  }

  // Issue #65: host-name resolution happens in the background task with
  // retries, so an unresolvable host (e.g. device offline at startup) no
  // longer prevents the appender from starting
  @Test
  public void startsWithUnresolvableRemoteHostAndRetriesResolution() throws Exception {

    new NetworkTestUtil().assumeNoUnresolvedUrlFallback();

    // given
    appender.setRemoteHost("NOT.A.VALID.REMOTE.HOST.NAME");

    // when
    appender.start();

    // then
    assertTrue(appender.isStarted());
    verify(appender, timeout(TIMEOUT)).addWarn(contains("unknown host"));
  }

  // Issue #341
  @Test
  public void lazyInitDefersConnectionUntilFirstEvent() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    appender.setLazy(true);

    // when
    appender.start();

    // then: started, but no connection attempt yet (the dispatch task is
    // only submitted on the first append, so nothing runs asynchronously)
    assertTrue(appender.isStarted());
    verify(appender, never()).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());

    // when
    appender.append("some event");

    // then
    verify(appender, timeout(TIMEOUT)).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());
  }

  @Test
  public void startsButOutputsWarningWhenQueueSizeIsZero() throws Exception {

    // given
    appender.setQueueSize(0);

    // when
    appender.start();

    // then
    assertTrue(appender.isStarted());
    verify(appender).addWarn("Queue size of zero is deprecated, use a size of one to indicate synchronous processing");
  }

  @Test
  public void startsWithValidParameters() throws Exception {

    // when
    appender.start();

    // then
    assertTrue(appender.isStarted());
  }

  @Test
  public void createsSocketConnectorWithConfiguredParameters() throws Exception {

    // given
    appender.setReconnectionDelay(new Duration(42));
    appender.setRemoteHost("localhost");
    appender.setPort(21);

    // when
    appender.start();

    // then
    verify(appender, timeout(TIMEOUT)).newConnector(InetAddress.getByName("localhost"), 21, 0, 42);
  }

  @Test
  public void addsInfoMessageWhenSocketConnectionWasEstablished() throws Exception {

    // when
    mockOneSuccessfulSocketConnection();
    appender.start();

    // then
    verify(appender, timeout(TIMEOUT)).addInfo(contains("connection established"));
  }

  @Test
  public void addsInfoMessageWhenSocketConnectionFailed() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(appender, timeout(TIMEOUT).atLeastOnce()).addInfo(contains("connection failed"));
  }

  @Test
  public void closesSocketOnException() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));
    appender.start();

    // when
    appender.append("some event");

    // then
    // Synchronize on the "connection closed" status message, which the connect
    // thread logs on the appender immediately after closing the socket, before
    // verifying the socket mock. Verifying the socket directly with a bare
    // timeout() races with the background thread and flakily reports zero
    // interactions under JDK 17 + Mockito's inline mock maker; awaiting the
    // appender signal first establishes the necessary happens-before ordering.
    verify(appender, timeout(TIMEOUT).atLeastOnce()).addInfo(contains("connection closed"));
    verify(socket).close();
  }

  @Test
  public void addsInfoMessageWhenSocketConnectionClosed() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(appender, timeout(TIMEOUT).atLeastOnce()).addInfo(contains("connection closed"));
  }

  @Test
  public void shutsDownOnInterruptWhileWaitingForEvent() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new InterruptedException()).when(deque).takeFirst();

    // when
    appender.start();

    // then
    verify(deque, timeout(TIMEOUT)).takeFirst();
  }

  @Test
  public void shutsDownOnInterruptWhileWaitingForSocketConnection() throws Exception {

    // given
    doThrow(new InterruptedException()).when(socketConnector).call();

    // when
    appender.start();

    // then
    verify(socketConnector, timeout(TIMEOUT)).call();
  }

  @Test
  public void addsInfoMessageWhenShuttingDownDueToInterrupt() throws Exception {

    // given
    doThrow(new InterruptedException()).when(socketConnector).call();

    // when
    appender.start();

    // then
    verify(appender, timeout(TIMEOUT)).addInfo(contains("shutting down"));
  }

  @Test
  public void offersEventsToTheEndOfTheDeque() throws Exception {

    // given
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(deque).offer(eq("some event"), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void doesNotQueueAnyEventsWhenStopped() throws Exception {

    // given
    appender.start();
    appender.stop();

    // when
    appender.append("some event");

    // then
    verifyNoInteractions(deque);
  }

  @Test
  public void addsInfoMessageWhenEventCouldNotBeQueuedInConfiguredTimeoutDueToQueueSizeLimitation() throws Exception {

    // given
    long eventDelayLimit = 42;
    doReturn(false).when(deque).offer("some event", eventDelayLimit, TimeUnit.MILLISECONDS);
    appender.setEventDelayLimit(new Duration(eventDelayLimit));
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(appender).addInfo("Dropping event due to timeout limit of [" + eventDelayLimit + " milliseconds] being exceeded");
  }

  @Test
  public void takesEventsFromTheFrontOfTheDeque() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    appender.start();
    awaitStartOfEventDispatching();

    // when
    appender.append("some event");

    // then
    verify(deque, timeout(TIMEOUT).atLeastOnce()).takeFirst();
  }

  @Test
  public void reAddsEventAtTheFrontOfTheDequeWhenTransmissionFails() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(objectWriter).write(any());
    appender.start();
    awaitStartOfEventDispatching();

    // when
    appender.append("some event");

    // then
    verify(deque, timeout(TIMEOUT).atLeastOnce()).offerFirst("some event");
  }

  @Test
  public void addsErrorMessageWhenAppendingIsInterruptedWhileWaitingForTheQueueToAcceptTheEvent() throws Exception {

    // given
    final InterruptedException interruptedException = new InterruptedException();
    doThrow(interruptedException).when(deque).offer(eq("some event"), anyLong(), any(TimeUnit.class));
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(appender).addError("Interrupted while appending event to SocketAppender", interruptedException);
  }

  @Test
  public void postProcessesEventsBeforeTransformingItToASerializable() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    appender.start();
    awaitStartOfEventDispatching();

    // when
    appender.append("some event");
    awaitAtLeastOneEventToBeDispatched();

    // then
    InOrder inOrder = inOrder(appender, preSerializationTransformer);
    inOrder.verify(appender).postProcessEvent("some event");
    inOrder.verify(preSerializationTransformer).transform("some event");
  }

  @Test
  public void writesSerializedEventToStream() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    when(preSerializationTransformer.transform("some event")).thenReturn("some serialized event");
    appender.start();
    awaitStartOfEventDispatching();

    // when
    appender.append("some event");

    // then
    verify(objectWriter, timeout(TIMEOUT)).write("some serialized event");
  }

  @Test
  public void addsInfoMessageWhenEventIsBeingDroppedBecauseOfConnectionProblemAndDequeCapacityLimitReached() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(objectWriter).write(any());
    doReturn(false).when(deque).offerFirst("some event");
    appender.start();
    awaitStartOfEventDispatching();
    reset(appender);

    // when
    appender.append("some event");

    // then
    verify(appender, timeout(TIMEOUT)).addInfo("Dropping event due to socket connection error and maxed out deque capacity");
  }

  @Test
  public void reEstablishesSocketConnectionOnConnectionDropWhenWritingEvent() throws Exception {

    // given
    mockTwoSuccessfulSocketConnections();
    doThrow(new IOException()).when(objectWriter).write(any());
    appender.start();
    awaitStartOfEventDispatching();

    // when
    appender.append("some event");

    // then
    verify(objectWriterFactory, timeout(TIMEOUT).atLeast(2)).newAutoFlushingObjectWriter(any(OutputStream.class));
  }

  @Test
  public void triesToReEstablishSocketConnectionIfItFailed() throws Exception {

    // given
    mockOneSuccessfulSocketConnection();
    doThrow(new IOException()).when(socket).getOutputStream();
    appender.start();

    // when
    appender.append("some event");

    // then
    verify(socketConnector, timeout(TIMEOUT).atLeast(2)).call();
  }

  @Test
  public void usesConfiguredAcceptConnectionTimeoutAndResetsSocketTimeoutAfterSuccessfulConnection() throws Exception {

    // when
    mockOneSuccessfulSocketConnection();
    appender.setAcceptConnectionTimeout(42);
    appender.start();
    awaitStartOfEventDispatching();

    // then
    InOrder inOrder = inOrder(socket);
    inOrder.verify(socket).setSoTimeout(42);
    inOrder.verify(socket).setSoTimeout(0);
  }

  @Test
  public void ignoresStartWhenAlreadyStarted() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();

    // when
    appender.start();

    // then
    assertTrue(appender.isStarted());
    verify(queueFactory, times(1)).<String>newLinkedBlockingDeque(anyInt());
  }

  @Test
  public void stopsLazyAppenderThatNeverSubmittedItsDispatchTask() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();

    // when
    appender.stop();

    // then
    assertFalse(appender.isStarted());
    verify(appender, never()).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());
  }

  @Test
  public void ignoresNullEvents() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();

    // when
    appender.append(null);

    // then: nothing is queued, and a lazy appender still does not connect
    verifyNoInteractions(deque);
    verify(appender, never()).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());
  }

  @Test
  public void givesUpResolvingRemoteHostWhenReconnectionDelayIsZero() throws Exception {

    // given
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost(UNRESOLVABLE_HOST);
    syncAppender.setReconnectionDelay(new Duration(0));

    // when
    syncAppender.start();

    // then
    assertTrue(syncAppender.isStarted());
    String peerId = "remote peer " + UNRESOLVABLE_HOST + ":" + AbstractSocketAppender.DEFAULT_PORT + ": ";
    verify(syncAppender).addWarn(peerId + "unknown host: " + UNRESOLVABLE_HOST + " (will retry)");
    verify(syncAppender).addError(peerId + "gave up resolving host (reconnectionDelay is zero)");
    verify(syncAppender, never()).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());
  }

  @Test
  public void givesUpResolvingRemoteHostWhenReconnectionDelayIsUnset() throws Exception {

    // given
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost(UNRESOLVABLE_HOST);
    syncAppender.setReconnectionDelay(null);

    // when
    syncAppender.start();

    // then
    verify(syncAppender).addError(contains("gave up resolving host (reconnectionDelay is zero)"));
    verify(syncAppender, never()).newConnector(any(InetAddress.class), anyInt(), anyLong(), anyLong());
  }

  @Test
  public void retriesHostResolutionAfterReconnectionDelay() throws Exception {

    // given: the host becomes resolvable after the first failed attempt
    // (e.g. a device that was offline at startup comes online)
    final InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost(UNRESOLVABLE_HOST);
    syncAppender.setReconnectionDelay(new Duration(1));
    doAnswer(invocation -> {
      syncAppender.setRemoteHost("127.0.0.1");
      return invocation.callRealMethod();
    }).when(syncAppender).addWarn(contains("unknown host"));

    // when
    syncAppender.start();

    // then
    verify(syncAppender, times(1)).addWarn(contains("unknown host"));
    verify(syncAppender, never()).addError(anyString());
    verify(syncAppender).newConnector(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}),
        AbstractSocketAppender.DEFAULT_PORT, 0, 1);
  }

  @Test
  public void createsConnectorWithoutRetryDelayWhenReconnectionDelayIsUnset() throws Exception {

    // given
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost("127.0.0.1");
    syncAppender.setPort(21);
    syncAppender.setReconnectionDelay(null);

    // when
    syncAppender.start();

    // then
    verify(syncAppender).newConnector(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 21, 0, 0);
  }

  @Test
  public void waitsBeforeReconnectingAfterSslHandshakeFailure() throws Exception {

    // given: the handshake of the only connection fails; the pending interrupt
    // then ends the wait before reconnecting (instead of sleeping 30 seconds)
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost("127.0.0.1");
    mockOneSuccessfulSocketConnection();
    doAnswer(invocation -> {
      Thread.currentThread().interrupt();
      throw new SSLHandshakeException("handshake failed");
    }).when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));

    // when
    boolean interruptPending;
    try {
      syncAppender.start();
    } finally {
      interruptPending = Thread.interrupted();
    }

    // then: the wait consumed the interrupt and shut the dispatcher down,
    // without handling the handshake failure as a plain connection failure
    assertFalse(interruptPending);
    verify(socketConnector, times(1)).call();
    verify(socket).close();
    verify(syncAppender, never()).addInfo(contains("connection failed"));
    InOrder inOrder = inOrder(syncAppender);
    inOrder.verify(syncAppender).addInfo("remote peer 127.0.0.1:" + AbstractSocketAppender.DEFAULT_PORT + ": connection closed");
    inOrder.verify(syncAppender).addInfo("shutting down");
  }

  @Test
  public void reconnectsAfterWaitingFollowingSslHandshakeFailure() throws Exception {

    // given
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();
    syncAppender.setRemoteHost("127.0.0.1");
    syncAppender.setHandshakeFailureDelay(1);
    mockOneSuccessfulSocketConnection();
    doThrow(new SSLHandshakeException("handshake failed"))
        .when(objectWriterFactory).newAutoFlushingObjectWriter(any(OutputStream.class));

    // when
    syncAppender.start();

    // then: after the wait it asks the connector for a new connection
    verify(socketConnector, times(2)).call();
    verify(socket).close();
    verify(syncAppender, never()).addInfo(contains("connection failed"));
    verify(syncAppender).addInfo("remote peer 127.0.0.1:" + AbstractSocketAppender.DEFAULT_PORT + ": connection closed");
  }

  @Test
  public void reportsInterruptedConnector() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();

    // when
    appender.connectionFailed(socketConnector, new InterruptedException());

    // then
    verify(appender).addInfo("connector interrupted");
  }

  @Test
  public void reportsRefusedConnection() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();

    // when
    appender.connectionFailed(socketConnector, new ConnectException("Connection refused"));

    // then
    verify(appender).addInfo("remote peer localhost:" + AbstractSocketAppender.DEFAULT_PORT + ": connection refused");
  }

  @Test
  public void reportsOtherConnectionFailuresWithTheException() throws Exception {

    // given
    appender.setLazy(true);
    appender.start();
    IOException failure = new IOException("network is unreachable");

    // when
    appender.connectionFailed(socketConnector, failure);

    // then
    verify(appender).addInfo("remote peer localhost:" + AbstractSocketAppender.DEFAULT_PORT + ": " + failure);
  }

  @Test
  public void hasDefaultProperties() throws Exception {

    // when
    InstrumentedSocketAppender syncAppender = newAppenderDispatchingOnCallingThread();

    // then
    assertFalse(syncAppender.getLazy());
    assertNull(syncAppender.getRemoteHost());
    assertEquals(AbstractSocketAppender.DEFAULT_PORT, syncAppender.getPort());
    assertEquals(AbstractSocketAppender.DEFAULT_RECONNECTION_DELAY, syncAppender.getReconnectionDelay().getMilliseconds());
    assertEquals(AbstractSocketAppender.DEFAULT_QUEUE_SIZE, syncAppender.getQueueSize());
    assertEquals(100, syncAppender.getEventDelayLimit().getMilliseconds());
  }

  @Test
  public void returnsConfiguredProperties() throws Exception {

    // given
    Duration reconnectionDelay = new Duration(42);
    Duration eventDelayLimit = new Duration(7);

    // when
    appender.setLazy(true);
    appender.setRemoteHost("some.host");
    appender.setPort(1234);
    appender.setReconnectionDelay(reconnectionDelay);
    appender.setQueueSize(5);
    appender.setEventDelayLimit(eventDelayLimit);

    // then
    assertTrue(appender.getLazy());
    assertEquals("some.host", appender.getRemoteHost());
    assertEquals(1234, appender.getPort());
    assertSame(reconnectionDelay, appender.getReconnectionDelay());
    assertEquals(5, appender.getQueueSize());
    assertSame(eventDelayLimit, appender.getEventDelayLimit());
  }

  /**
   * Creates an appender whose context runs the dispatch task synchronously on
   * the thread that submits it, so that {@code start()} returns only once the
   * task has finished (the mock connector yields no socket unless stubbed).
   */
  private InstrumentedSocketAppender newAppenderDispatchingOnCallingThread() {
    inlineAppender = spy(new InstrumentedSocketAppender(preSerializationTransformer, queueFactory, objectWriterFactory, socketConnector));
    inlineAppender.setContext(new MockContext(new MockScheduledExecutorService()));
    return inlineAppender;
  }

  private void awaitAtLeastOneEventToBeDispatched() throws IOException {
    verify(objectWriter, timeout(TIMEOUT)).write(anyString());
  }

  private void awaitStartOfEventDispatching() throws InterruptedException {
    verify(deque, timeout(TIMEOUT)).takeFirst();
  }

  private void mockOneSuccessfulSocketConnection() throws InterruptedException {
    doReturn(socket).doReturn(null).when(socketConnector).call();
  }

  private void mockTwoSuccessfulSocketConnections() throws InterruptedException {
    doReturn(socket).doReturn(socket).doReturn(null).when(socketConnector).call();
  }
}
