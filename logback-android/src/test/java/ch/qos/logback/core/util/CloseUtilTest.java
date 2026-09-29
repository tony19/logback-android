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
package ch.qos.logback.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

/**
 * Unit tests for {@link CloseUtil}.
 */
public class CloseUtilTest {

  @Test
  public void isInstantiable() {
    // the class only has static members, but its implicit constructor is public
    assertNotNull(new CloseUtil());
  }

  @Test
  public void closeableNullIsIgnored() {
    CloseUtil.closeQuietly((Closeable) null);
  }

  @Test
  public void closeableIsClosed() {
    CountingCloseable closeable = new CountingCloseable(null);
    CloseUtil.closeQuietly(closeable);
    assertEquals(1, closeable.closeCount);
  }

  @Test
  public void closeableIOExceptionIsSuppressed() {
    CountingCloseable closeable = new CountingCloseable(new IOException("close failed"));
    CloseUtil.closeQuietly(closeable);
    assertEquals(1, closeable.closeCount);
  }

  @Test
  public void socketNullIsIgnored() {
    CloseUtil.closeQuietly((Socket) null);
  }

  @Test
  public void socketIsClosed() {
    Socket socket = new Socket();
    CloseUtil.closeQuietly(socket);
    assertTrue(socket.isClosed());
  }

  @Test
  public void socketIOExceptionIsSuppressed() {
    FailingCloseSocket socket = new FailingCloseSocket();
    CloseUtil.closeQuietly(socket);
    assertEquals(1, socket.closeCount);
    assertTrue(socket.isClosed());
  }

  @Test
  public void serverSocketNullIsIgnored() {
    CloseUtil.closeQuietly((ServerSocket) null);
  }

  @Test
  public void serverSocketIsClosed() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    CloseUtil.closeQuietly(serverSocket);
    assertTrue(serverSocket.isClosed());
  }

  @Test
  public void serverSocketIOExceptionIsSuppressed() throws IOException {
    FailingCloseServerSocket serverSocket = new FailingCloseServerSocket();
    CloseUtil.closeQuietly(serverSocket);
    assertEquals(1, serverSocket.closeCount);
    assertTrue(serverSocket.isClosed());
  }

  private static class CountingCloseable implements Closeable {
    private final IOException toThrow;
    int closeCount;

    CountingCloseable(IOException toThrow) {
      this.toThrow = toThrow;
    }

    @Override
    public void close() throws IOException {
      closeCount++;
      if (toThrow != null) {
        throw toThrow;
      }
    }
  }

  /** Closes the real socket, then reports a failure. */
  private static class FailingCloseSocket extends Socket {
    int closeCount;

    @Override
    public synchronized void close() throws IOException {
      closeCount++;
      super.close();
      throw new IOException("close failed");
    }
  }

  /** Closes the real server socket, then reports a failure. */
  private static class FailingCloseServerSocket extends ServerSocket {
    int closeCount;

    FailingCloseServerSocket() throws IOException {
      super();
    }

    @Override
    public void close() throws IOException {
      closeCount++;
      super.close();
      throw new IOException("close failed");
    }
  }
}
