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
package ch.qos.logback.classic.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.MockServerListener;
import ch.qos.logback.core.net.server.MockServerRunner;
import ch.qos.logback.core.net.server.ServerListener;
import ch.qos.logback.core.net.server.ServerRunner;
import ch.qos.logback.core.net.server.ServerSocketUtil;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

/**
 * Unit tests for {@link ServerSocketReceiver}.
 *
 * @author Carl Harris
 */
public class ServerSocketReceiverTest {

  private MockContext context = new MockContext();

  private MockServerRunner<RemoteAppenderClient> runner =
      new MockServerRunner<RemoteAppenderClient>();

  private MockServerListener<RemoteAppenderClient> listener =
      new MockServerListener<RemoteAppenderClient>();

  private ServerSocket serverSocket;
  private InstrumentedServerSocketReceiver receiver;

  @Before
  public void setUp() throws Exception {
    serverSocket = ServerSocketUtil.createServerSocket();
    receiver = new InstrumentedServerSocketReceiver(serverSocket, listener, runner);
    receiver.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    serverSocket.close();
  }

  @Test
  public void testStartStop() throws Exception {
    receiver.start();
    assertTrue(runner.isContextInjected());
    assertTrue(runner.isRunning());
    assertSame(listener, receiver.getLastListener());

    receiver.stop();
    assertFalse(runner.isRunning());
  }

  @Test
  public void testStartWhenAlreadyStarted() throws Exception {
    receiver.start();
    receiver.start();
    assertEquals(1, runner.getStartCount());
  }

  @Test
  public void testStopThrowsException() throws Exception {
    receiver.start();
    assertTrue(receiver.isStarted());
    IOException ex = new IOException("test exception");
    runner.setStopException(ex);
    receiver.stop();

    Status status = context.getLastStatus();
    assertNotNull(status);
    assertTrue(status instanceof ErrorStatus);
    assertTrue(status.getMessage().contains(ex.getMessage()));
    assertSame(ex, status.getThrowable());
  }

  @Test
  public void testStopWhenNotStarted() throws Exception {
    receiver.stop();
    assertEquals(0, runner.getStartCount());
  }

  @Test
  public void startupFailureIsReportedAndClosesTheServerSocket() throws Exception {
    final RuntimeException failure = new IllegalStateException("no listener");
    ServerSocketReceiver failing = new InstrumentedServerSocketReceiver(serverSocket, listener, runner) {
      @Override
      protected ServerListener<RemoteAppenderClient> createServerListener(ServerSocket socket) {
        throw failure;
      }
    };
    failing.setContext(context);

    failing.start();

    assertFalse(failing.isStarted());
    assertEquals(0, runner.getStartCount());
    assertTrue(serverSocket.isClosed());
    Status status = context.getLastStatus();
    assertTrue(status instanceof ErrorStatus);
    assertEquals("server startup error: " + failure, status.getMessage());
    assertSame(failure, status.getThrowable());
  }

  @Test
  public void startOpensTheServerSocketOnTheConfiguredPortBacklogAndAddress() throws Exception {
    final List<Object> socketArgs = new ArrayList<Object>();
    ServerSocketReceiver configured = new ServerSocketReceiver() {
      @Override
      protected ServerSocketFactory getServerSocketFactory() {
        return new ServerSocketFactory() {
          @Override
          public ServerSocket createServerSocket(int port) {
            throw new UnsupportedOperationException();
          }

          @Override
          public ServerSocket createServerSocket(int port, int backlog) {
            throw new UnsupportedOperationException();
          }

          @Override
          public ServerSocket createServerSocket(int port, int backlog, InetAddress address) {
            socketArgs.add(port);
            socketArgs.add(backlog);
            socketArgs.add(address);
            return serverSocket;
          }
        };
      }

      @Override
      protected ServerRunner createServerRunner(ServerListener<RemoteAppenderClient> listener,
          Executor executor) {
        return runner;
      }
    };
    configured.setContext(context);
    configured.setPort(1234);
    configured.setBacklog(7);
    configured.setAddress("127.0.0.2");

    configured.start();

    assertTrue(configured.isStarted());
    assertEquals(1234, configured.getPort());
    assertEquals(7, configured.getBacklog());
    assertEquals("127.0.0.2", configured.getAddress());
    assertEquals(Arrays.<Object>asList(1234, 7, InetAddress.getByName("127.0.0.2")), socketArgs);
    configured.stop();
  }

  @Test
  public void defaultsListenOnAllInterfacesOfTheDefaultPort() throws Exception {
    ServerSocketReceiver defaults = new ServerSocketReceiver();
    assertEquals(AbstractSocketAppender.DEFAULT_PORT, defaults.getPort());
    assertEquals(ServerSocketReceiver.DEFAULT_BACKLOG, defaults.getBacklog());
    assertNull(defaults.getAddress());
    assertNull(defaults.getInetAddress());
    assertSame(ServerSocketFactory.getDefault(), defaults.getServerSocketFactory());
  }

  @Test
  public void defaultListenerOwnsTheServerSocket() throws Exception {
    ServerListener<RemoteAppenderClient> defaultListener =
        new ServerSocketReceiver().createServerListener(serverSocket);

    assertTrue(defaultListener instanceof RemoteAppenderServerListener);
    defaultListener.close();
    assertTrue(serverSocket.isClosed());
  }

  @Test
  public void onStopBeforeAnyStartDoesNothing() throws Exception {
    // there is no runner to stop yet
    receiver.onStop();
    assertNull(context.getLastStatus());
  }

}
