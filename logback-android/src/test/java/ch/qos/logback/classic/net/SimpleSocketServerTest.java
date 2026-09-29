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

import static ch.qos.logback.classic.net.SimpleSocketServerMainRule.CONFIG_FILE;
import static ch.qos.logback.classic.net.SimpleSocketServerMainRule.OUT_OF_RANGE_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.net.ServerSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.net.mock.MockAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.util.CloseUtil;

/**
 * Unit tests for {@link SimpleSocketServer}.
 */
public class SimpleSocketServerTest {

  private static final String SERVER_LOGGER = "simple-socket-server";
  private static final long TIMEOUT_MILLIS = 10000;

  private final LoggerContext lc = new LoggerContext();
  private final ListAppender<ILoggingEvent> serverLog = new ListAppender<ILoggingEvent>();
  private final List<ServerSocket> createdSockets = new ArrayList<ServerSocket>();
  private final String testThreadName = Thread.currentThread().getName();
  private SimpleSocketServer server;

  @Rule
  public final SimpleSocketServerMainRule main = new SimpleSocketServerMainRule();

  @Before
  public void setUp() {
    serverLog.setContext(lc);
    serverLog.start();
    Logger log = lc.getLogger(SERVER_LOGGER);
    log.setLevel(Level.DEBUG);
    log.addAppender(serverLog);
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.close();
    }
    for (ServerSocket socket : createdSockets) {
      CloseUtil.closeQuietly(socket);
    }
    Thread.currentThread().setName(testThreadName);
    RecordingServer.started.clear();
    lc.stop();
  }

  @Test
  public void serverDispatchesClientEventsAndStopsQuietlyWhenClosed() throws Exception {
    MockAppender serverEvents = new MockAppender();
    serverEvents.start();
    lc.getLogger(SERVER_LOGGER).addAppender(serverEvents);
    MockAppender remoteEvents = new MockAppender();
    remoteEvents.start();
    Logger remoteLogger = lc.getLogger("remote");
    remoteLogger.addAppender(remoteEvents);

    server = newServer(new LoopbackServerSocketFactory(false));
    server.start();
    assertTrue(awaitMessage(serverEvents, "Waiting to accept a new client."));

    Socket client = new Socket(InetAddress.getLoopbackAddress(), createdSockets.get(0).getLocalPort());
    try {
      ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
      oos.writeObject(LoggingEventVO.build(new LoggingEvent(getClass().getName(), remoteLogger,
          Level.INFO, "from the client", null, null)));
      oos.flush();

      ILoggingEvent received = remoteEvents.awaitAppend(TIMEOUT_MILLIS);
      assertNotNull(received);
      assertEquals("from the client", received.getMessage());
      assertEquals("remote", received.getLoggerName());
      // the server is back to accepting once the client's node is running
      assertTrue(awaitMessage(serverEvents, "Waiting to accept a new client."));

      server.close();
      server.join(TIMEOUT_MILLIS);
      // the client's node ends once the server closed it, and says so to the
      // server; after that, nothing else logs to the server's logger
      assertTrue(awaitMessageStartingWith(serverEvents, "Removing "));
    } finally {
      client.close();
    }

    assertFalse(server.isAlive());
    assertTrue(containsMessage(Level.INFO, "Starting new socket node."));
    assertTrue(containsMessage(Level.INFO,
        "Exception in run method for a closed server. This is normal."));
    assertTrue(eventsAt(Level.ERROR).isEmpty());
  }

  @Test
  public void runDoesNotAcceptClientsWhenClosedBeforeItStarts() throws Exception {
    server = newServer(new LoopbackServerSocketFactory(false));
    server.close();

    server.run();

    assertTrue(containsMessage(Level.INFO, "Listening on port 0"));
    assertFalse(containsMessage(Level.INFO, "Waiting to accept a new client."));
    assertEquals(1, createdSockets.size());
    assertEquals(testThreadName, Thread.currentThread().getName());
  }

  @Test
  public void closeClosesTheServerSocketOpenedByRun() throws Exception {
    server = newServer(new LoopbackServerSocketFactory(false));
    server.close();
    server.run();
    assertFalse(createdSockets.get(0).isClosed());

    server.close();

    assertTrue(createdSockets.get(0).isClosed());
    assertTrue(server.isClosed());
    assertTrue(eventsAt(Level.ERROR).isEmpty());
  }

  @Test
  public void closeLogsErrorWhenServerSocketFailsToClose() throws Exception {
    server = newServer(new LoopbackServerSocketFactory(true));
    server.close();
    server.run();

    server.close();
    // the socket that failed to close is dropped, so closing again does not retry it
    server.close();

    List<ILoggingEvent> errors = eventsAt(Level.ERROR);
    assertEquals(1, errors.size());
    assertEquals("Failed to close serverSocket", errors.get(0).getMessage());
    assertEquals("close failed", errors.get(0).getThrowableProxy().getMessage());
    assertTrue(createdSockets.get(0).isClosed());
  }

  @Test
  public void runLogsUnexpectedFailureWhenServerSocketCannotBeCreated() {
    server = newServer(new ServerSocketFactory() {
      @Override
      public ServerSocket createServerSocket(int port) throws IOException {
        throw new IOException("cannot listen");
      }

      @Override
      public ServerSocket createServerSocket(int port, int backlog) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ServerSocket createServerSocket(int port, int backlog, InetAddress address) {
        throw new UnsupportedOperationException();
      }
    });

    server.run();

    List<ILoggingEvent> errors = eventsAt(Level.ERROR);
    assertEquals(1, errors.size());
    assertEquals("Unexpected failure in run method", errors.get(0).getMessage());
    assertEquals("cannot listen", errors.get(0).getThrowableProxy().getMessage());
    assertFalse(containsMessage(Level.INFO, "Exception in run method for a closed server. This is normal."));
    assertFalse(server.isClosed());
    assertEquals(testThreadName, Thread.currentThread().getName());
  }

  @Test
  public void isClosedIsFalseUntilCloseIsCalled() {
    server = new SimpleSocketServer(lc, 0);
    assertFalse(server.isClosed());
    server.close();
    assertTrue(server.isClosed());
  }

  @Test
  public void signalAlmostReadinessCountsDownOnlyAPendingLatch() {
    server = new SimpleSocketServer(lc, 0);
    // without a latch there is nothing to signal
    server.signalAlmostReadiness();

    CountDownLatch latch = new CountDownLatch(1);
    server.setLatch(latch);
    assertSame(latch, server.getLatch());

    server.signalAlmostReadiness();
    assertEquals(0, latch.getCount());
    server.signalAlmostReadiness();
    assertEquals(0, latch.getCount());
  }

  @Test
  public void parsePortNumberParsesADecimalPort() {
    assertEquals(4560, SimpleSocketServer.parsePortNumber("4560"));
  }

  @Test
  public void parsePortNumberPrintsUsageAndExitsOnUnparsablePort() {
    Runtime runtime = mock(Runtime.class);
    String stderr;
    int port;
    try (MockedStatic<Runtime> runtimeStatic = mockStatic(Runtime.class)) {
      runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);
      StderrCapture capture = new StderrCapture();
      try {
        port = SimpleSocketServer.parsePortNumber("not-a-port");
      } finally {
        stderr = capture.stop();
      }
    }

    verify(runtime).exit(1);
    // only reached because the mocked exit returns
    assertEquals(-1, port);
    assertTrue(stderr, stderr.contains(NumberFormatException.class.getName()));
    assertTrue(stderr, stderr.contains("Could not interpret port number [not-a-port]."));
    assertTrue(stderr, stderr.contains(
        "Usage: java " + SimpleSocketServer.class.getName() + " port configFile"));
  }

  @Test
  public void mainPrintsUsageAndExitsOnWrongNumberOfArguments() {
    Runtime runtime = mock(Runtime.class);
    String stderr;
    try (MockedStatic<Runtime> runtimeStatic = mockStatic(Runtime.class)) {
      runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);
      StderrCapture capture = new StderrCapture();
      try {
        // the mocked exit returns, so main runs on and trips over the missing
        // config file argument
        assertThrows(ArrayIndexOutOfBoundsException.class, new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            SimpleSocketServer.main(new String[] {"4560"});
          }
        });
      } finally {
        stderr = capture.stop();
      }
    }

    verify(runtime).exit(1);
    assertEquals("Wrong number of arguments." + System.lineSeparator()
        + "Usage: java " + SimpleSocketServer.class.getName() + " port configFile"
        + System.lineSeparator(), stderr);
  }

  @Test
  public void mainPrintsUsageAndExitsOnTooManyArguments() throws Throwable {
    Runtime runtime = mock(Runtime.class);
    String stderr;
    try (MockedStatic<Runtime> runtimeStatic = mockStatic(Runtime.class)) {
      runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);
      StderrCapture capture = new StderrCapture();
      try {
        main.run(() -> SimpleSocketServer.main(new String[] {"4560", CONFIG_FILE, "extra"}));
      } finally {
        stderr = capture.stop();
      }
    }

    verify(runtime).exit(1);
    assertTrue(stderr, stderr.startsWith("Wrong number of arguments." + System.lineSeparator()));
    // only reached because the mocked exit returns: the port was never parsed
    assertEquals("Logback SimpleSocketServer (port -1)", main.awaitServerFailure().getThreadName());
  }

  @Test
  public void mainConfiguresTheDefaultContextFromTheFileAndStartsASocketServerOnThePort()
      throws Throwable {
    main.run(() -> SimpleSocketServer.main(new String[] {OUT_OF_RANGE_PORT, CONFIG_FILE}));

    main.assertConfiguredDefaultContextFrom(CONFIG_FILE);
    ILoggingEvent failure = main.awaitServerFailure();
    assertEquals("Logback SimpleSocketServer (port " + OUT_OF_RANGE_PORT + ")",
        failure.getThreadName());
    assertEquals(IllegalArgumentException.class.getName(),
        failure.getThrowableProxy().getClassName());
    assertTrue(main.logged("Listening on port " + OUT_OF_RANGE_PORT));
  }

  @Test
  public void doMainStartsAServerOfTheGivenClassForTheDefaultContextOnThePort() throws Throwable {
    main.run(() -> SimpleSocketServer.doMain(RecordingServer.class,
        new String[] {"4560", CONFIG_FILE}));

    main.assertConfiguredDefaultContextFrom(CONFIG_FILE);
    assertEquals(1, RecordingServer.started.size());
    RecordingServer started = RecordingServer.started.get(0);
    assertSame(main.defaultContext(), started.context);
    assertEquals(4560, started.port);
  }

  @Test
  public void configureLCResetsTheContextThenConfiguresItFromTheFile() throws Exception {
    final Logger stale = lc.getLogger("stale");
    stale.setLevel(Level.ERROR);
    final List<Level> staleLevelWhenConfigured = new ArrayList<Level>();
    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(
        JoranConfigurator.class, (configurator, context) -> doAnswer(invocation -> {
          staleLevelWhenConfigured.add(stale.getLevel());
          return null;
        }).when(configurator).doConfigure("server.xml"))) {

      SimpleSocketServer.configureLC(lc, "server.xml");

      assertEquals(1, configurators.constructed().size());
      JoranConfigurator configurator = configurators.constructed().get(0);
      InOrder inOrder = inOrder(configurator);
      inOrder.verify(configurator).setContext(lc);
      inOrder.verify(configurator).doConfigure("server.xml");
    }
    // the context was reset before it was configured
    assertEquals(Collections.<Level>singletonList(null), staleLevelWhenConfigured);
  }

  private SimpleSocketServer newServer(final ServerSocketFactory factory) {
    SimpleSocketServer server = new SimpleSocketServer(lc, 0) {
      @Override
      protected ServerSocketFactory getServerSocketFactory() {
        return factory;
      }
    };
    server.logger = lc.getLogger(SERVER_LOGGER);
    return server;
  }

  private static boolean awaitMessage(MockAppender appender, String message)
      throws InterruptedException {
    ILoggingEvent event;
    while ((event = appender.awaitAppend(TIMEOUT_MILLIS)) != null) {
      if (message.equals(event.getFormattedMessage())) {
        return true;
      }
    }
    return false;
  }

  private static boolean awaitMessageStartingWith(MockAppender appender, String prefix)
      throws InterruptedException {
    ILoggingEvent event;
    while ((event = appender.awaitAppend(TIMEOUT_MILLIS)) != null) {
      if (event.getFormattedMessage().startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsMessage(Level level, String message) {
    for (ILoggingEvent event : eventsAt(level)) {
      if (message.equals(event.getFormattedMessage())) {
        return true;
      }
    }
    return false;
  }

  private List<ILoggingEvent> eventsAt(Level level) {
    List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();
    for (ILoggingEvent event : serverLog.list) {
      if (event.getLevel() == level) {
        events.add(event);
      }
    }
    return events;
  }

  /**
   * Creates loopback server sockets on an ephemeral port and remembers them;
   * optionally the sockets fail to close (after actually closing).
   */
  private final class LoopbackServerSocketFactory extends ServerSocketFactory {
    private final boolean failOnClose;

    LoopbackServerSocketFactory(boolean failOnClose) {
      this.failOnClose = failOnClose;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
      ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress()) {
        @Override
        public void close() throws IOException {
          super.close();
          if (failOnClose) {
            throw new IOException("close failed");
          }
        }
      };
      createdSockets.add(socket);
      return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress address) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * A server that {@code doMain} can create; it records that it was started
   * instead of listening.
   */
  public static class RecordingServer extends SimpleSocketServer {
    static final List<RecordingServer> started = new ArrayList<RecordingServer>();

    final LoggerContext context;
    final int port;

    public RecordingServer(LoggerContext context, int port) {
      super(context, port);
      this.context = context;
      this.port = port;
    }

    @Override
    public synchronized void start() {
      started.add(this);
    }
  }

  /** Captures what is printed to {@link System#err} until {@link #stop()}. */
  private static final class StderrCapture {
    private final PrintStream original = System.err;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    StderrCapture() {
      try {
        System.setErr(new PrintStream(bytes, true, "UTF-8"));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    String stop() {
      System.setErr(original);
      try {
        return bytes.toString("UTF-8");
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
