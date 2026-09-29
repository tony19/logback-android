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
package ch.qos.logback.core.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ServerSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Unit tests for {@link AbstractServerSocketAppender}.
 *
 * @author Carl Harris
 */
public class AbstractServerSocketAppenderTest {


  private MockContext context = new MockContext();

  private MockServerRunner<RemoteReceiverClient> runner =
      new MockServerRunner<RemoteReceiverClient>();

  private MockServerListener<RemoteReceiverClient> listener =
      new MockServerListener<RemoteReceiverClient>();

  private ServerSocket serverSocket;
  private InstrumentedServerSocketAppenderBase appender;

  // collaborators of the RecordingServerSocketAppender tests below
  private final ScheduledExecutorService executor =
      mock(ScheduledExecutorService.class);

  private final ContextBase recordingContext = new ContextBase() {
    @Override
    public synchronized ScheduledExecutorService getScheduledExecutorService() {
      return executor;
    }
  };

  private final StatusChecker checker = new StatusChecker(recordingContext);

  private final ServerSocketFactory socketFactory =
      mock(ServerSocketFactory.class);

  @SuppressWarnings("unchecked")
  private final ServerRunner<RemoteReceiverClient> mockRunner =
      mock(ServerRunner.class);

  private final RecordingServerSocketAppender recordingAppender =
      new RecordingServerSocketAppender();

  private InetAddress loopback;
  private ServerSocket unboundSocket;

  @Before
  public void setUp() throws Exception {
    serverSocket = ServerSocketUtil.createServerSocket();
    appender = new InstrumentedServerSocketAppenderBase(serverSocket, listener, runner);
    appender.setContext(context);

    loopback = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
    unboundSocket = new ServerSocket();
    when(socketFactory.createServerSocket(anyInt(), anyInt(), any()))
        .thenReturn(unboundSocket);
    recordingAppender.setContext(recordingContext);
  }

  @After
  public void tearDown() throws Exception {
    serverSocket.close();
    unboundSocket.close();
    if (recordingAppender.lastSocket != null) {
      recordingAppender.lastSocket.close();
    }
  }

  @Test
  public void testStartStop() throws Exception {
    appender.start();
    assertTrue(runner.isContextInjected());
    assertTrue(runner.isRunning());
    assertSame(listener, appender.getLastListener());

    appender.stop();
    assertFalse(runner.isRunning());
  }

  @Test
  public void testStartWhenAlreadyStarted() throws Exception {
    appender.start();
    appender.start();
    assertEquals(1, runner.getStartCount());
  }

  @Test
  public void testStopThrowsException() throws Exception {
    appender.start();
    assertTrue(appender.isStarted());
    IOException ex = new IOException("test exception");
    runner.setStopException(ex);
    appender.stop();

    Status status = context.getLastStatus();
    assertNotNull(status);
    assertTrue(status instanceof ErrorStatus);
    assertTrue(status.getMessage().contains(ex.getMessage()));
    assertSame(ex, status.getThrowable());
  }

  @Test
  public void testStopWhenNotStarted() throws Exception {
    appender.stop();
    assertEquals(0, runner.getStartCount());
  }

  @Test
  public void listenerSettingsHaveDefaultsAndCanBeChanged() {
    assertEquals(AbstractSocketAppender.DEFAULT_PORT,
        recordingAppender.getPort());
    assertEquals(AbstractServerSocketAppender.DEFAULT_BACKLOG,
        recordingAppender.getBacklog());
    assertEquals(AbstractServerSocketAppender.DEFAULT_CLIENT_QUEUE_SIZE,
        recordingAppender.getClientQueueSize());
    assertNull(recordingAppender.getAddress());

    recordingAppender.setPort(4561);
    recordingAppender.setBacklog(7);
    recordingAppender.setClientQueueSize(3);
    recordingAppender.setAddress("127.0.0.1");

    assertEquals(4561, recordingAppender.getPort());
    assertEquals(7, recordingAppender.getBacklog());
    assertEquals(3, recordingAppender.getClientQueueSize());
    assertEquals("127.0.0.1", recordingAppender.getAddress());
  }

  @Test
  public void defaultServerSocketFactoryIsThePlatformDefault()
      throws Exception {
    assertSame(ServerSocketFactory.getDefault(),
        recordingAppender.getServerSocketFactory());
  }

  @Test
  public void inetAddressIsNullWhenNoAddressIsSet() throws Exception {
    assertNull(recordingAppender.getInetAddress());
  }

  @Test
  public void inetAddressIsResolvedFromTheConfiguredAddress() throws Exception {
    recordingAppender.setAddress("127.0.0.1");
    assertEquals(loopback, recordingAppender.getInetAddress());
  }

  @Test
  public void startCreatesTheServerSocketFromTheListenerSettings()
      throws Exception {
    recordingAppender.socketFactory = socketFactory;
    recordingAppender.setPort(4561);
    recordingAppender.setBacklog(7);
    recordingAppender.setAddress("127.0.0.1");

    recordingAppender.start();

    assertTrue(recordingAppender.isStarted());
    verify(socketFactory).createServerSocket(4561, 7, loopback);
    assertSame(unboundSocket, recordingAppender.lastSocket);
    ArgumentCaptor<Runnable> command = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(command.capture());
    assertTrue(command.getValue() instanceof RemoteReceiverServerRunner);
    assertSame(recordingContext,
        ((RemoteReceiverServerRunner) command.getValue()).getContext());
  }

  @Test
  public void startListensOnTheConfiguredAddressAndStopClosesTheSocket()
      throws Exception {
    recordingAppender.setAddress("127.0.0.1");
    recordingAppender.setPort(0);

    recordingAppender.start();

    assertTrue(recordingAppender.isStarted());
    assertTrue(recordingAppender.lastSocket.isBound());
    assertEquals(loopback, recordingAppender.lastSocket.getInetAddress());

    recordingAppender.stop();

    assertFalse(recordingAppender.isStarted());
    assertTrue(recordingAppender.lastSocket.isClosed());
  }

  @Test
  public void startFailureIsReportedAndLeavesTheAppenderStopped()
      throws Exception {
    BindException ex = new BindException("Address already in use");
    when(socketFactory.createServerSocket(anyInt(), anyInt(), any()))
        .thenThrow(ex);
    recordingAppender.socketFactory = socketFactory;

    recordingAppender.start();

    assertFalse(recordingAppender.isStarted());
    checker.assertContainsMatch(Status.ERROR,
        "server startup error: java.net.BindException: Address already in use");
    assertTrue(checker.containsException(BindException.class));
    verifyNoInteractions(executor);
  }

  @Test
  public void appendIgnoresNullEvents() throws Exception {
    recordingAppender.socketFactory = socketFactory;
    recordingAppender.runner = mockRunner;
    recordingAppender.start();

    recordingAppender.doAppend(null);

    assertTrue(recordingAppender.postProcessed.isEmpty());
    verify(mockRunner, never()).accept(any());
    checker.assertIsErrorFree();
  }

  @Test
  public void appendOffersTheTransformedEventToEachClient() throws Exception {
    final RemoteReceiverClient client1 = mock(RemoteReceiverClient.class);
    final RemoteReceiverClient client2 = mock(RemoteReceiverClient.class);
    doAnswer(invocation -> {
      ClientVisitor<RemoteReceiverClient> visitor = invocation.getArgument(0);
      visitor.visit(client1);
      visitor.visit(client2);
      return null;
    }).when(mockRunner).accept(any());
    recordingAppender.socketFactory = socketFactory;
    recordingAppender.runner = mockRunner;
    recordingAppender.start();

    recordingAppender.doAppend("event");

    assertEquals(1, recordingAppender.postProcessed.size());
    assertEquals("event", recordingAppender.postProcessed.get(0));
    verify(client1).offer("serialized event");
    verify(client2).offer("serialized event");
  }

  @Test
  public void defaultServerRunnerGivesEachClientAQueueOfTheConfiguredSize() {
    recordingAppender.setClientQueueSize(2);
    @SuppressWarnings("unchecked")
    ServerListener<RemoteReceiverClient> serverListener =
        mock(ServerListener.class);

    ServerRunner<RemoteReceiverClient> defaultRunner =
        recordingAppender.createServerRunner(serverListener, executor);

    assertTrue(defaultRunner instanceof RemoteReceiverServerRunner);
    RemoteReceiverStreamClient client =
        new RemoteReceiverStreamClient("id", new ByteArrayOutputStream());
    assertTrue(((RemoteReceiverServerRunner) defaultRunner)
        .configureClient(client));
    assertTrue(client.offer("1"));
    assertTrue(client.offer("2"));
    assertFalse(client.offer("3"));
  }

  /**
   * An {@link AbstractServerSocketAppender} that records what it is given,
   * and uses the base class's socket factory and server runner unless a
   * replacement is set.
   */
  private static class RecordingServerSocketAppender
      extends AbstractServerSocketAppender<String> {

    final List<String> postProcessed = new ArrayList<String>();
    ServerSocketFactory socketFactory;
    ServerRunner<RemoteReceiverClient> runner;
    ServerSocket lastSocket;

    @Override
    protected void postProcessEvent(String event) {
      postProcessed.add(event);
    }

    @Override
    protected PreSerializationTransformer<String> getPST() {
      return new PreSerializationTransformer<String>() {
        @Override
        public Serializable transform(String event) {
          return "serialized " + event;
        }
      };
    }

    @Override
    protected ServerSocketFactory getServerSocketFactory() throws Exception {
      return socketFactory != null
          ? socketFactory : super.getServerSocketFactory();
    }

    @Override
    protected ServerListener<RemoteReceiverClient> createServerListener(
        ServerSocket socket) {
      lastSocket = socket;
      return super.createServerListener(socket);
    }

    @Override
    protected ServerRunner<RemoteReceiverClient> createServerRunner(
        ServerListener<RemoteReceiverClient> listener, Executor executor) {
      return runner != null
          ? runner : super.createServerRunner(listener, executor);
    }
  }

}
