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
package ch.qos.logback.classic.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.mock.MockAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.net.server.ServerSocketUtil;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.NetworkTestUtil;

/**
 * Unit tests for {@link SocketReceiver}.
 *
 * @author Carl Harris
 */
public class SocketReceiverTest {

  private static final int DELAY = 1000;
  private static final String TEST_HOST_NAME = "NOT.A.VALID.HOST.NAME";
  private static final String LOOPBACK_ADDRESS = "127.0.0.1";
  private static final int REMOTE_PORT = 6000;
  private static final String RECEIVER_ID =
      "receiver " + LOOPBACK_ADDRESS + ":" + REMOTE_PORT + ": ";


  private ServerSocket serverSocket;
  private Socket socket;
  private MockSocketFactory socketFactory = new MockSocketFactory();
  private MockSocketConnector connector;
  private MockAppender appender;
  private LoggerContext lc;
  private Logger logger;

  private InstrumentedSocketReceiver receiver =
      new InstrumentedSocketReceiver();

  @Before
  public void setUp() throws Exception {
    serverSocket = ServerSocketUtil.createServerSocket();
    socket = new Socket(serverSocket.getInetAddress(),
        serverSocket.getLocalPort());
    connector = new MockSocketConnector(socket);

    lc = new LoggerContext();
    lc.reset();
    receiver.setContext(lc);
    appender = new MockAppender();
    appender.start();
    logger = lc.getLogger(getClass());
    logger.addAppender(appender);
  }

  @After
  public void tearDown() throws Exception {
    receiver.stop();
    ExecutorService executor = lc.getScheduledExecutorService();
    executor.shutdownNow();
    assertTrue(executor.awaitTermination(DELAY, TimeUnit.MILLISECONDS));
    socket.close();
    serverSocket.close();
    lc.stop();
  }

  @Test
  public void testStartNoRemoteAddress() throws Exception {
    receiver.start();
    assertFalse(receiver.isStarted());
    int count = lc.getStatusManager().getCount();
    Status status = lc.getStatusManager().getCopyOfStatusList().get(count - 1);
    assertTrue(status.getMessage().contains("host"));
  }

  @Test
  public void testStartNoPort() throws Exception {
    receiver.setRemoteHost(TEST_HOST_NAME);
    receiver.start();
    assertFalse(receiver.isStarted());
    int count = lc.getStatusManager().getCount();
    Status status = lc.getStatusManager().getCopyOfStatusList().get(count - 1);
    assertTrue(status.getMessage().contains("port"));
  }

  @Test
  public void testStartUnknownHost() throws Exception {
    new NetworkTestUtil().assumeNoUnresolvedUrlFallback();
    receiver.setPort(6000);
    receiver.setRemoteHost(TEST_HOST_NAME);
    receiver.start();
    assertFalse(receiver.isStarted());
    int count = lc.getStatusManager().getCount();
    Status status = lc.getStatusManager().getCopyOfStatusList().get(count - 1);
    assertTrue(status.getMessage().contains("unknown host"));
  }

  @Test
  public void testStartStop() throws Exception {
    receiver.setRemoteHost(InetAddress.getLocalHost().getHostName());
    receiver.setPort(6000);
    receiver.setAcceptConnectionTimeout(DELAY / 2);
    receiver.start();
    assertTrue(receiver.isStarted());
    receiver.awaitConnectorCreated(DELAY);
    receiver.stop();
    assertFalse(receiver.isStarted());
  }

  @Test
  public void testServerSlowToAcceptConnection() throws Exception {
    receiver.setRemoteHost(InetAddress.getLocalHost().getHostName());
    receiver.setPort(6000);
    receiver.setAcceptConnectionTimeout(DELAY / 4);
    receiver.start();
    assertTrue(receiver.awaitConnectorCreated(DELAY / 2));
    // note that we don't call serverSocket.accept() here
    // but processPriorToRemoval (in tearDown) should still clean up everything
  }

  @Test
  public void testServerDropsConnection() throws Exception {
    receiver.setRemoteHost(InetAddress.getLocalHost().getHostName());
    receiver.setPort(6000);
    receiver.start();
    assertTrue(receiver.awaitConnectorCreated(DELAY));
    Socket socket = serverSocket.accept();
    socket.close();
  }

  @Test
  public void testDispatchEventForEnabledLevel() throws Exception {
    receiver.setRemoteHost(InetAddress.getLocalHost().getHostName());
    receiver.setPort(6000);
    receiver.start();
    assertTrue(receiver.awaitConnectorCreated(DELAY));
    Socket socket = serverSocket.accept();

    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

    logger.setLevel(Level.DEBUG);
    ILoggingEvent event = new LoggingEvent(logger.getName(), logger,
        Level.DEBUG, "test message", null, new Object[0]);

    LoggingEventVO eventVO = LoggingEventVO.build(event);
    oos.writeObject(eventVO);
    oos.flush();

    ILoggingEvent rcvdEvent = appender.awaitAppend(DELAY);
    assertNotNull(rcvdEvent);
    assertEquals(event.getLoggerName(), rcvdEvent.getLoggerName());
    assertEquals(event.getLevel(), rcvdEvent.getLevel());
    assertEquals(event.getMessage(), rcvdEvent.getMessage());
  }

  @Test
  public void testNoDispatchEventForDisabledLevel() throws Exception {
    receiver.setRemoteHost(InetAddress.getLocalHost().getHostName());
    receiver.setPort(6000);
    receiver.start();
    assertTrue(receiver.awaitConnectorCreated(DELAY));
    Socket socket = serverSocket.accept();

    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
    logger.setLevel(Level.INFO);
    ILoggingEvent event = new LoggingEvent(logger.getName(), logger,
        Level.DEBUG, "test message", null, new Object[0]);

    LoggingEventVO eventVO = LoggingEventVO.build(event);
    oos.writeObject(eventVO);
    oos.flush();

    assertNull(appender.awaitAppend(DELAY));
  }

  @Test
  public void runEndsWhenTheExecutorRejectsTheConnector() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();
    lc.getScheduledExecutorService().shutdown();

    scripted.run();

    assertEquals(1, scripted.connectors.size());
    assertEquals(0, scripted.connectors.get(0).calls);
    assertEquals("shutting down", lastStatus().getMessage());
  }

  @Test
  public void runEndsWhenTheConnectorFails() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();

    scripted.run();

    assertEquals(1, scripted.connectors.size());
    assertEquals(1, scripted.connectors.get(0).calls);
    assertEquals("shutting down", lastStatus().getMessage());
    new StatusChecker(lc).assertNoMatch("connection established");
  }

  @Test
  public void runEndsWhenInterruptedWhileWaitingForTheConnector() throws Exception {
    final CountDownLatch release = new CountDownLatch(1);
    ScriptedSocketReceiver scripted = new ScriptedSocketReceiver() {
      @Override
      protected SocketConnector newConnector(InetAddress address, int port,
          int initialDelay, int retryDelay) {
        // run() is on this (the test) thread: interrupt it before it waits
        // for a connector that can't finish, so that the wait is always
        // interrupted rather than only when stop() happens to land there
        Thread.currentThread().interrupt();
        return new ScriptedConnector(null) {
          @Override
          public Socket call() {
            try {
              release.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return null;
          }
        };
      }
    };
    configure(scripted);

    try {
      scripted.run();
    } finally {
      release.countDown();
    }

    // the wait consumed the interrupt
    assertFalse(Thread.interrupted());
    assertEquals("shutting down", lastStatus().getMessage());
    new StatusChecker(lc).assertNoMatch("connection established");
  }

  @Test
  public void dispatchReportsEndOfStreamThenReconnects() throws Exception {
    logger.setLevel(Level.DEBUG);
    FakeSocket peer = new FakeSocket(serialize(LoggingEventVO.build(new LoggingEvent(
        logger.getName(), logger, Level.INFO, "over the wire", null, null))));
    ScriptedSocketReceiver scripted = newScriptedReceiver(peer);
    scripted.setAcceptConnectionTimeout(1234);

    scripted.run();

    ILoggingEvent received = appender.getLastEvent();
    assertEquals("over the wire", received.getMessage());
    assertEquals(Arrays.asList(1234, 0), peer.soTimeouts);
    assertTrue(peer.isClosed());
    StatusChecker checker = new StatusChecker(lc);
    checker.assertContainsMatch(Status.INFO, "^" + RECEIVER_ID + "connection established$");
    checker.assertContainsMatch(Status.INFO, "^" + RECEIVER_ID + "end-of-stream detected$");
    checker.assertContainsMatch(Status.INFO, "^" + RECEIVER_ID + "connection closed$");
    // once the connection closed, the receiver connected again (which failed)
    assertEquals(2, scripted.connectors.size());
    assertEquals("shutting down", lastStatus().getMessage());
  }

  @Test
  public void dispatchReportsUnknownEventClass() throws Exception {
    FakeSocket peer = new FakeSocket(serializedObjectOfMissingClass("does.not.Exist"));
    ScriptedSocketReceiver scripted = newScriptedReceiver(peer);

    scripted.run();

    assertNull(appender.getLastEvent());
    assertTrue(peer.isClosed());
    StatusChecker checker = new StatusChecker(lc);
    checker.assertContainsMatch(Status.INFO, "^" + RECEIVER_ID
        + "unknown event class: java.lang.ClassNotFoundException: does.not.Exist$");
    checker.assertContainsMatch(Status.INFO, "^" + RECEIVER_ID + "connection closed$");
  }

  @Test
  public void connectorsRetryAfterTheDefaultReconnectionDelay() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();

    scripted.run();

    assertEquals(Arrays.asList(AbstractSocketAppender.DEFAULT_RECONNECTION_DELAY),
        scripted.retryDelays);
  }

  @Test
  public void connectorsRetryAfterTheConfiguredReconnectionDelay() throws Exception {
    ScriptedSocketReceiver scripted = new ScriptedSocketReceiver();
    scripted.setReconnectionDelay(1234);
    configure(scripted);

    scripted.run();

    assertEquals(Arrays.asList(1234), scripted.retryDelays);
  }

  @Test
  public void connectorsUseThePlatformSocketFactoryAndReportToTheReceiver() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();

    scripted.run();

    ScriptedConnector used = scripted.connectors.get(0);
    assertSame(SocketFactory.getDefault(), used.socketFactory);
    assertSame(scripted, used.exceptionHandler);
  }

  @Test
  public void defaultConnectorConnectsToTheRemoteServer() throws Exception {
    SocketConnector defaultConnector = new SocketReceiver().newConnector(
        serverSocket.getInetAddress(), serverSocket.getLocalPort(), 0, DELAY);
    assertTrue(defaultConnector instanceof DefaultSocketConnector);

    Socket connected = defaultConnector.call();
    try {
      assertTrue(connected.isConnected());
      assertEquals(serverSocket.getLocalPort(), connected.getPort());
    } finally {
      connected.close();
    }
  }

  @Test
  public void connectionFailedReportsInterruption() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();
    InterruptedException ex = new InterruptedException();

    scripted.connectionFailed(null, ex);

    assertWarning("connector interrupted", ex);
  }

  @Test
  public void connectionFailedReportsRefusedConnection() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();
    ConnectException ex = new ConnectException("refused");

    scripted.connectionFailed(null, ex);

    assertWarning(RECEIVER_ID + "connection refused", ex);
  }

  @Test
  public void connectionFailedReportsOtherErrors() throws Exception {
    ScriptedSocketReceiver scripted = newScriptedReceiver();
    IOException ex = new IOException("other");

    scripted.connectionFailed(null, ex);

    assertWarning(RECEIVER_ID + "unspecified error", ex);
  }

  private ScriptedSocketReceiver newScriptedReceiver(Socket... sockets) {
    ScriptedSocketReceiver scripted = new ScriptedSocketReceiver();
    scripted.sockets.addAll(Arrays.asList(sockets));
    configure(scripted);
    return scripted;
  }

  private void configure(ScriptedSocketReceiver scripted) {
    scripted.setContext(lc);
    scripted.setRemoteHost(LOOPBACK_ADDRESS);
    scripted.setPort(REMOTE_PORT);
    // resolves the address and names the receiver, as start() would
    assertTrue(scripted.shouldStart());
  }

  private Status lastStatus() {
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    return statuses.get(statuses.size() - 1);
  }

  private void assertWarning(String message, Throwable throwable) {
    Status status = lastStatus();
    assertEquals(Status.WARN, status.getLevel());
    assertEquals(message, status.getMessage());
    assertSame(throwable, status.getThrowable());
  }

  private static byte[] serialize(Object... objects) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bytes);
    for (Object object : objects) {
      oos.writeObject(object);
    }
    oos.close();
    return bytes.toByteArray();
  }

  /**
   * A serialization stream holding one object of a proxy class that
   * implements a missing interface: reading it fails with
   * {@link ClassNotFoundException}.
   */
  private static byte[] serializedObjectOfMissingClass(String interfaceName) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    out.writeShort(ObjectStreamConstants.STREAM_MAGIC);
    out.writeShort(ObjectStreamConstants.STREAM_VERSION);
    out.writeByte(ObjectStreamConstants.TC_OBJECT);
    out.writeByte(ObjectStreamConstants.TC_PROXYCLASSDESC);
    out.writeInt(1);
    out.writeUTF(interfaceName);
    out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
    out.writeByte(ObjectStreamConstants.TC_NULL);
    out.close();
    return bytes.toByteArray();
  }

  /**
   * A {@link SocketReceiver} whose connectors hand out the given sockets, one
   * per connection attempt, and fail once there are none left.
   */
  private static class ScriptedSocketReceiver extends SocketReceiver {
    final Deque<Socket> sockets = new ArrayDeque<Socket>();
    final List<ScriptedConnector> connectors = new ArrayList<ScriptedConnector>();
    final List<Integer> retryDelays = new ArrayList<Integer>();

    @Override
    protected SocketConnector newConnector(InetAddress address, int port,
        int initialDelay, int retryDelay) {
      retryDelays.add(retryDelay);
      ScriptedConnector connector = new ScriptedConnector(sockets.poll());
      connectors.add(connector);
      return connector;
    }
  }

  private static class ScriptedConnector implements SocketConnector {
    private final Socket socket;
    volatile int calls;
    ExceptionHandler exceptionHandler;
    SocketFactory socketFactory;

    ScriptedConnector(Socket socket) {
      this.socket = socket;
    }

    @Override
    public Socket call() {
      calls++;
      if (socket == null) {
        throw new IllegalStateException("no more connections");
      }
      return socket;
    }

    @Override
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
      this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void setSocketFactory(SocketFactory socketFactory) {
      this.socketFactory = socketFactory;
    }
  }

  /** An unconnected socket that serves the given bytes, then end-of-stream. */
  private static class FakeSocket extends Socket {
    private final InputStream in;
    final List<Integer> soTimeouts = new ArrayList<Integer>();

    FakeSocket(byte[] data) {
      this.in = new ByteArrayInputStream(data);
    }

    @Override
    public InputStream getInputStream() {
      return in;
    }

    @Override
    public void setSoTimeout(int timeout) {
      soTimeouts.add(timeout);
    }
  }

  /**
   * A {@link SocketReceiver} with instrumentation for unit testing.
   */
  private class InstrumentedSocketReceiver extends SocketReceiver {

    private boolean connectorCreated;

    @Override
    protected synchronized SocketConnector newConnector(
        InetAddress address, int port, int initialDelay, int retryDelay) {
      connectorCreated = true;
      notifyAll();
      return connector;
    }

    @Override
    protected SocketFactory getSocketFactory() {
      return socketFactory;
    }

    public synchronized boolean awaitConnectorCreated(long delay)
        throws InterruptedException {
      while (!connectorCreated) {
        wait(delay);
      }
      return connectorCreated;
    }

  }

  /**
   * A {@link SocketConnector} with instrumentation for unit testing.
   */
  private static class MockSocketConnector implements SocketConnector {

    private final Socket socket;

    public MockSocketConnector(Socket socket) {
      this.socket = socket;
    }

    public Socket call() throws InterruptedException {
      return socket;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
    }

    public void setSocketFactory(SocketFactory socketFactory) {
    }

  }

  /**
   * A no-op {@link SocketFactory} to support unit testing.
   */
  private static class MockSocketFactory extends SocketFactory {

    @Override
    public Socket createSocket(InetAddress address, int port,
        InetAddress localAddress, int localPort) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
        int localPort) throws IOException, UnknownHostException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException,
        UnknownHostException {
      throw new UnsupportedOperationException();
    }

  }
}
