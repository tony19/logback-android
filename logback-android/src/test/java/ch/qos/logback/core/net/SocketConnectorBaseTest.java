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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.core.net.SocketConnector.ExceptionHandler;
import ch.qos.logback.core.net.server.ServerSocketUtil;

/**
 * Unit tests for {@link SocketConnectorBase}.
 *
 * @author Carl Harris
 */
@Ignore()
public class SocketConnectorBaseTest {

  private static final int DELAY = 1000;

  private MockExceptionHandler exceptionHandler = new MockExceptionHandler();

  private ServerSocket serverSocket;
  private SocketConnectorBase connector;

  @Before
  public void setUp() throws Exception {
    serverSocket = ServerSocketUtil.createServerSocket();
    connector = new SocketConnectorBase(serverSocket.getInetAddress(),
        serverSocket.getLocalPort(), 0, DELAY);
    connector.setExceptionHandler(exceptionHandler);
  }

  @After
  public void tearDown() throws Exception {
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  @Test
  public void testConnect() throws Exception {
    Thread thread = new Thread();
    thread.start();
    Socket socket = connector.awaitConnection(2 * DELAY);
    assertNotNull(socket);
    thread.join(DELAY);
    assertFalse(thread.isAlive());
    socket.close();
  }

  @Test
  public void testConnectionFails() throws Exception {
    serverSocket.close();
    Thread thread = new Thread();
    thread.start();
    Socket socket = connector.awaitConnection(2 * DELAY);
    assertNull(socket);
    Exception lastException = exceptionHandler.awaitConnectionFailed(DELAY);
    assertTrue(lastException instanceof ConnectException);
    assertTrue(thread.isAlive());
    thread.interrupt();
    thread.join(4 * DELAY);
    assertFalse(thread.isAlive());
  }

  @Test
  public void testConnectEventually() throws Exception {
    serverSocket.close();

    Thread thread = new Thread();
    thread.start();
    Socket socket = connector.awaitConnection(2 * DELAY);
    assertNull(socket);
    Exception lastException = exceptionHandler.awaitConnectionFailed(DELAY);
    assertTrue(lastException instanceof ConnectException);
    assertTrue(thread.isAlive());

    // now rebind to the same local address
    SocketAddress address = serverSocket.getLocalSocketAddress();
    serverSocket = new ServerSocket();
    serverSocket.setReuseAddress(true);
    serverSocket.bind(address);

    // now we should be able to connect
    socket = connector.awaitConnection(2 * DELAY);
    assertNotNull(socket);
    thread.join(DELAY);
    assertFalse(thread.isAlive());
    socket.close();
  }

  private static class MockExceptionHandler implements ExceptionHandler {

    private final Lock lock = new ReentrantLock();
    private final Condition failedCondition = lock.newCondition();

    private Exception lastException;

    public void connectionFailed(SocketConnector connector, Exception ex) {
      lastException = ex;
    }

    public Exception awaitConnectionFailed(long delay)
         throws InterruptedException {
      lock.lock();
      try {
        boolean timeout = false;
        while (lastException == null && !timeout) {
          timeout = !failedCondition.await(delay, TimeUnit.MILLISECONDS);
        }
        return lastException;
      }
      finally {
        lock.unlock();
      }
    }

  }

}
