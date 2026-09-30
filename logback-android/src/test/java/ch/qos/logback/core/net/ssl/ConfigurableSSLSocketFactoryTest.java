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
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;

/**
 * Unit tests for {@link ConfigurableSSLSocketFactory}.
 */
public class ConfigurableSSLSocketFactoryTest {

  private static final String[] PROTOCOLS = { "TLSv1.2", "TLSv1.3" };
  private static final String[] ENABLED_PROTOCOLS = { "TLSv1.3" };
  private static final String[] CIPHER_SUITES = { "SUITE_A", "SUITE_B" };
  private static final String[] ENABLED_CIPHER_SUITES = { "SUITE_B" };

  private final SSLSocketFactory delegate = mock(SSLSocketFactory.class);
  private final SSLSocket socket = mock(SSLSocket.class);
  private final SSLParametersConfiguration parameters =
      new SSLParametersConfiguration();
  private final ConfigurableSSLSocketFactory factory =
      new ConfigurableSSLSocketFactory(parameters, delegate);

  private InetAddress remote;
  private InetAddress local;

  @Before
  public void setUp() throws Exception {
    remote = InetAddress.getByAddress(new byte[] { 127, 0, 0, 2 });
    local = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
    parameters.setContext(new ContextBase());
    parameters.setIncludedProtocols("TLSv1.3");
    parameters.setIncludedCipherSuites("SUITE_B");
    parameters.setWantClientAuth(true);
    when(socket.getSupportedProtocols()).thenReturn(PROTOCOLS);
    when(socket.getEnabledProtocols()).thenReturn(PROTOCOLS);
    when(socket.getSupportedCipherSuites()).thenReturn(CIPHER_SUITES);
    when(socket.getEnabledCipherSuites()).thenReturn(CIPHER_SUITES);
  }

  @Test
  public void createSocketToAddressConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createSocket(remote, 4560)).thenReturn(socket);

    Socket created = factory.createSocket(remote, 4560);

    assertSame(socket, created);
    verifyConfigured();
  }

  @Test
  public void createSocketToAddressFromLocalPortConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createSocket(remote, 4560, local, 4561)).thenReturn(socket);

    Socket created = factory.createSocket(remote, 4560, local, 4561);

    assertSame(socket, created);
    verifyConfigured();
  }

  @Test
  public void createSocketToHostConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createSocket("logs.example.com", 4560)).thenReturn(socket);

    Socket created = factory.createSocket("logs.example.com", 4560);

    assertSame(socket, created);
    verifyConfigured();
  }

  @Test
  public void createSocketToHostFromLocalPortConfiguresTheDelegatesSocket()
      throws Exception {
    when(delegate.createSocket("logs.example.com", 4560, local, 4561))
        .thenReturn(socket);

    Socket created =
        factory.createSocket("logs.example.com", 4560, local, 4561);

    assertSame(socket, created);
    verifyConfigured();
  }

  private void verifyConfigured() {
    verify(socket).setEnabledProtocols(ENABLED_PROTOCOLS);
    verify(socket).setEnabledCipherSuites(ENABLED_CIPHER_SUITES);
    verify(socket).setWantClientAuth(true);
  }

}
