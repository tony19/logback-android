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
package ch.qos.logback.core.net.ssl;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;

/**
 * Unit tests for {@link ConfigurableSSLServerSocketFactory}.
 */
public class ConfigurableSSLServerSocketFactoryTest {

  private static final String[] PROTOCOLS = { "TLSv1.2", "TLSv1.3" };
  private static final String[] ENABLED_PROTOCOLS = { "TLSv1.3" };
  private static final String[] CIPHER_SUITES = { "SUITE_A", "SUITE_B" };
  private static final String[] ENABLED_CIPHER_SUITES = { "SUITE_B" };

  private final SSLServerSocketFactory delegate =
      mock(SSLServerSocketFactory.class);
  private final SSLServerSocket socket = mock(SSLServerSocket.class);
  private final SSLParametersConfiguration parameters =
      new SSLParametersConfiguration();
  private final ConfigurableSSLServerSocketFactory factory =
      new ConfigurableSSLServerSocketFactory(parameters, delegate);

  @Before
  public void setUp() {
    parameters.setContext(new ContextBase());
    parameters.setIncludedProtocols("TLSv1.3");
    parameters.setIncludedCipherSuites("SUITE_B");
    parameters.setNeedClientAuth(true);
    when(socket.getSupportedProtocols()).thenReturn(PROTOCOLS);
    when(socket.getEnabledProtocols()).thenReturn(PROTOCOLS);
    when(socket.getSupportedCipherSuites()).thenReturn(CIPHER_SUITES);
    when(socket.getEnabledCipherSuites()).thenReturn(CIPHER_SUITES);
  }

  @Test
  public void createServerSocketWithPortConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createServerSocket(4560)).thenReturn(socket);

    ServerSocket created = factory.createServerSocket(4560);

    assertSame(socket, created);
    verifyConfigured();
  }

  @Test
  public void createServerSocketWithPortAndBacklogConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createServerSocket(4560, 7)).thenReturn(socket);

    ServerSocket created = factory.createServerSocket(4560, 7);

    assertSame(socket, created);
    verifyConfigured();
  }

  @Test
  public void createServerSocketWithAddressConfiguresTheDelegatesSocket()
      throws Exception {
    InetAddress address = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
    when(delegate.createServerSocket(4560, 7, address)).thenReturn(socket);

    ServerSocket created = factory.createServerSocket(4560, 7, address);

    assertSame(socket, created);
    verifyConfigured();
  }

  private void verifyConfigured() {
    verify(socket).setEnabledProtocols(ENABLED_PROTOCOLS);
    verify(socket).setEnabledCipherSuites(ENABLED_CIPHER_SUITES);
    verify(socket).setNeedClientAuth(true);
  }

}
