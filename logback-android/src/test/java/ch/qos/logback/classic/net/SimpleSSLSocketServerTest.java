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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.junit.After;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Unit tests for {@link SimpleSSLSocketServer}.
 * <p>
 * Its {@code main} is tested in {@link SimpleSocketServerMainTest}.
 */
public class SimpleSSLSocketServerTest {

  private final LoggerContext lc = new LoggerContext();

  @After
  public void tearDown() {
    lc.stop();
  }

  @Test
  public void constructorRejectsMissingSslContext() {
    NullPointerException ex = assertThrows(NullPointerException.class, new ThrowingRunnable() {
      @Override
      public void run() {
        new SimpleSSLSocketServer(lc, 0, null);
      }
    });
    assertEquals("SSL context required", ex.getMessage());
  }

  @Test
  public void serverSocketsComeFromTheGivenSslContext() throws Exception {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    SSLServerSocket socket = mock(SSLServerSocket.class);
    when(socket.getSupportedProtocols()).thenReturn(new String[] {"TLSv1.2"});
    when(socket.getEnabledProtocols()).thenReturn(new String[] {"TLSv1.2"});
    when(socket.getSupportedCipherSuites()).thenReturn(new String[] {"CIPHER"});
    when(socket.getEnabledCipherSuites()).thenReturn(new String[] {"CIPHER"});
    SSLServerSocketFactory sslFactory = mock(SSLServerSocketFactory.class);
    when(sslFactory.createServerSocket(4560, 7, loopback)).thenReturn(socket);
    SSLContext sslContext = mock(SSLContext.class);
    when(sslContext.getServerSocketFactory()).thenReturn(sslFactory);

    SimpleSSLSocketServer server = new SimpleSSLSocketServer(lc, 4560, sslContext);
    ServerSocket created = server.getServerSocketFactory().createServerSocket(4560, 7, loopback);

    assertSame(socket, created);
    // the server's SSL parameters were applied to the socket
    verify(socket).setEnabledProtocols(new String[] {"TLSv1.2"});
    verify(socket).setEnabledCipherSuites(new String[] {"CIPHER"});
    new StatusChecker(lc).assertContainsMatch(Status.INFO, "enabled protocol: TLSv1.2");
  }

  @Test
  public void defaultSslContextServesSslServerSockets() throws Exception {
    SimpleSSLSocketServer server = new SimpleSSLSocketServer(lc, 0);

    ServerSocket socket = server.getServerSocketFactory()
        .createServerSocket(0, 1, InetAddress.getLoopbackAddress());
    try {
      assertTrue(socket instanceof SSLServerSocket);
      new StatusChecker(lc).assertContainsMatch(Status.INFO, "hostnameVerification=false");
    } finally {
      socket.close();
    }
  }
}
