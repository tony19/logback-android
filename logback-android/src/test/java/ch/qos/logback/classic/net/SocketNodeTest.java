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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.HardenedLoggingEventInputStream;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.read.ListAppender;

/**
 * Unit tests for {@link SocketNode}. The node reads from a fake socket on the
 * test thread, so every outcome is deterministic.
 */
public class SocketNodeTest {

  private static final SocketAddress PEER = new InetSocketAddress(InetAddress.getLoopbackAddress(), 4321);

  private final LoggerContext lc = new LoggerContext();
  private final ListAppender<ILoggingEvent> nodeLog = new ListAppender<ILoggingEvent>();
  private final ListAppender<ILoggingEvent> remoteEvents = new ListAppender<ILoggingEvent>();
  private final List<SocketNode> closingNodes = new ArrayList<SocketNode>();
  private final SimpleSocketServer server = new SimpleSocketServer(lc, 0) {
    @Override
    public void socketNodeClosing(SocketNode sn) {
      closingNodes.add(sn);
    }
  };
  private Logger remoteLogger;

  @Before
  public void setUp() {
    nodeLog.setContext(lc);
    nodeLog.start();
    lc.getLogger(SocketNode.class).addAppender(nodeLog);
    remoteEvents.setContext(lc);
    remoteEvents.start();
    remoteLogger = lc.getLogger("remote");
    remoteLogger.addAppender(remoteEvents);
  }

  @After
  public void tearDown() {
    lc.stop();
  }

  @Test
  public void runDispatchesOnlyEventsEnabledForTheLocalLogger() throws Exception {
    remoteLogger.setLevel(Level.INFO);
    FakeSocket socket = new FakeSocket(new ScriptedInputStream(
        serialize(event(Level.DEBUG, "dropped"), event(Level.WARN, "kept")), null));
    SocketNode node = new SocketNode(server, socket, lc);

    node.run();

    assertEquals(1, remoteEvents.list.size());
    assertEquals("kept", remoteEvents.list.get(0).getMessage());
    assertEquals(Level.WARN, remoteEvents.list.get(0).getLevel());
    assertLogged(Level.INFO, "Caught java.io.EOFException closing connection.");
    assertNodeClosed(node, socket);
  }

  @Test
  public void runLogsErrorAndClosesWhenTheInputStreamCannotBeOpened() {
    final IOException failure = new IOException("no stream");
    FakeSocket socket = new FakeSocket(null) {
      @Override
      public InputStream getInputStream() throws IOException {
        throw failure;
      }
    };
    SocketNode node = new SocketNode(server, socket, lc);

    node.run();

    ILoggingEvent error = assertLogged(Level.ERROR, "Could not open ObjectInputStream to " + socket);
    assertEquals("no stream", error.getThrowableProxy().getMessage());
    assertEquals(1, nodeLog.list.size());
    assertTrue(remoteEvents.list.isEmpty());
    assertTrue(node.closed);
    assertNull(node.hardenedLoggingEventInputStream);
    assertEquals(1, closingNodes.size());
    assertSame(node, closingNodes.get(0));
  }

  @Test
  public void runLogsSocketExceptionAndCloses() throws Exception {
    FakeSocket socket = new FakeSocket(new ScriptedInputStream(serialize(),
        new SocketException("connection reset")));
    SocketNode node = new SocketNode(server, socket, lc);

    node.run();

    assertLogged(Level.INFO, "Caught java.net.SocketException closing connection.");
    assertNodeClosed(node, socket);
  }

  @Test
  public void runLogsOtherIOExceptionAndCloses() throws Exception {
    FakeSocket socket = new FakeSocket(new ScriptedInputStream(serialize(),
        new IOException("broken pipe")));
    SocketNode node = new SocketNode(server, socket, lc);

    node.run();

    assertLogged(Level.INFO, "Caught java.io.IOException: java.io.IOException: broken pipe");
    assertLogged(Level.INFO, "Closing connection.");
    assertNodeClosed(node, socket);
  }

  @Test
  public void runLogsUnexpectedExceptionForAnObjectThatIsNotAnEvent() throws Exception {
    FakeSocket socket = new FakeSocket(new ScriptedInputStream(serialize("not an event"), null));
    SocketNode node = new SocketNode(server, socket, lc);

    node.run();

    ILoggingEvent error = assertLogged(Level.ERROR, "Unexpected exception. Closing connection.");
    assertEquals(ClassCastException.class.getName(), error.getThrowableProxy().getClassName());
    assertTrue(remoteEvents.list.isEmpty());
    assertNodeClosed(node, socket);
  }

  @Test
  public void closeWarnsWhenTheInputStreamFailsToClose() throws Exception {
    ScriptedInputStream in = new ScriptedInputStream(serialize(), null);
    in.closeFailure = new IOException("cannot close");
    SocketNode node = new SocketNode(server, new FakeSocket(null), lc);
    node.hardenedLoggingEventInputStream = new HardenedLoggingEventInputStream(in);

    node.close();

    ILoggingEvent warning = assertLogged(Level.WARN, "Could not close connection.");
    assertEquals("cannot close", warning.getThrowableProxy().getMessage());
    assertTrue(node.closed);
    assertNull(node.hardenedLoggingEventInputStream);
  }

  @Test
  public void closeBeforeRunOnlyMarksTheNodeClosed() {
    SocketNode node = new SocketNode(server, new FakeSocket(null), lc);

    node.close();
    node.close();

    assertTrue(node.closed);
    assertTrue(nodeLog.list.isEmpty());
  }

  @Test
  public void toStringNamesTheRemotePeer() {
    SocketNode node = new SocketNode(server, new FakeSocket(null), lc);
    assertEquals(SocketNode.class.getName() + PEER, node.toString());
  }

  private void assertNodeClosed(SocketNode node, FakeSocket socket) {
    assertTrue(node.closed);
    assertNull(node.hardenedLoggingEventInputStream);
    assertTrue("socket input stream was not closed", socket.in.closed);
    assertEquals(1, closingNodes.size());
    assertSame(node, closingNodes.get(0));
    for (ILoggingEvent event : nodeLog.list) {
      assertFalse(event.getFormattedMessage(), event.getLevel() == Level.WARN);
    }
  }

  private ILoggingEvent assertLogged(Level level, String message) {
    for (ILoggingEvent event : nodeLog.list) {
      if (event.getLevel() == level && message.equals(event.getFormattedMessage())) {
        return event;
      }
    }
    throw new AssertionError("no " + level + " [" + message + "] in " + nodeLog.list);
  }

  private LoggingEventVO event(Level level, String message) {
    return LoggingEventVO.build(new LoggingEvent(getClass().getName(), remoteLogger, level,
        message, null, null));
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
   * Serves the given bytes, then either signals end-of-stream or throws the
   * given failure; can also fail to close.
   */
  private static class ScriptedInputStream extends InputStream {
    private final byte[] data;
    private final IOException failure;
    private int pos;
    IOException closeFailure;
    boolean closed;

    ScriptedInputStream(byte[] data, IOException failure) {
      this.data = data;
      this.failure = failure;
    }

    @Override
    public int read() throws IOException {
      byte[] one = new byte[1];
      return read(one, 0, 1) == -1 ? -1 : one[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (pos == data.length) {
        if (failure != null) {
          throw failure;
        }
        return -1;
      }
      int n = Math.min(len, data.length - pos);
      System.arraycopy(data, pos, b, off, n);
      pos += n;
      return n;
    }

    @Override
    public void close() throws IOException {
      closed = true;
      if (closeFailure != null) {
        throw closeFailure;
      }
    }
  }

  /** An unconnected socket that reports {@link #PEER} and serves a scripted stream. */
  private static class FakeSocket extends Socket {
    final ScriptedInputStream in;

    FakeSocket(ScriptedInputStream in) {
      this.in = in;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return in;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
      return PEER;
    }
  }
}
