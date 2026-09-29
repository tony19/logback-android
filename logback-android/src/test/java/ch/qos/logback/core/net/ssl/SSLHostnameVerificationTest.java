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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;

/**
 * Tests the hostnameVerification default and its API level guard.
 */
@RunWith(RobolectricTestRunner.class)
public class SSLHostnameVerificationTest {

  private static final String[] NONE = new String[0];

  private final ContextBase context = new ContextBase();
  private final SSLParametersConfiguration configuration = new SSLParametersConfiguration();
  private final SSLSocket socket = mock(SSLSocket.class);
  private final SSLServerSocket serverSocket = mock(SSLServerSocket.class);

  @Before
  public void setUp() {
    configuration.setContext(context);
    when(socket.getEnabledProtocols()).thenReturn(NONE);
    when(socket.getSupportedProtocols()).thenReturn(NONE);
    when(socket.getEnabledCipherSuites()).thenReturn(NONE);
    when(socket.getSupportedCipherSuites()).thenReturn(NONE);
    when(socket.getSSLParameters()).thenReturn(new SSLParameters());
    when(serverSocket.getEnabledProtocols()).thenReturn(NONE);
    when(serverSocket.getSupportedProtocols()).thenReturn(NONE);
    when(serverSocket.getEnabledCipherSuites()).thenReturn(NONE);
    when(serverSocket.getSupportedCipherSuites()).thenReturn(NONE);
    when(serverSocket.getSSLParameters()).thenReturn(new SSLParameters());
  }

  @Test
  @Config(sdk = 29)
  public void clientSocketVerifiesHostnameByDefault() {
    configuration.configure(new SSLConfigurableSocket(socket));

    ArgumentCaptor<SSLParameters> captor = ArgumentCaptor.forClass(SSLParameters.class);
    verify(socket).setSSLParameters(captor.capture());
    assertEquals("HTTPS", captor.getValue().getEndpointIdentificationAlgorithm());
  }

  @Test
  @Config(sdk = 29)
  public void clientSocketHostnameVerificationCanBeDisabled() {
    configuration.setHostnameVerification(false);
    configuration.configure(new SSLConfigurableSocket(socket));

    verify(socket, never()).setSSLParameters(any(SSLParameters.class));
  }

  @Test
  @Config(sdk = 29)
  public void serverSocketDoesNotVerifyHostnameByDefault() {
    configuration.configure(new SSLConfigurableServerSocket(serverSocket));

    verify(serverSocket, never()).setSSLParameters(any(SSLParameters.class));
  }

  @Test
  @Config(sdk = 29)
  public void serverSocketVerifiesHostnameWhenEnabled() {
    configuration.setHostnameVerification(true);
    configuration.configure(new SSLConfigurableServerSocket(serverSocket));

    ArgumentCaptor<SSLParameters> captor = ArgumentCaptor.forClass(SSLParameters.class);
    verify(serverSocket).setSSLParameters(captor.capture());
    assertEquals("HTTPS", captor.getValue().getEndpointIdentificationAlgorithm());
  }

  @Test
  @Config(sdk = 23)
  public void belowApi24HostnameIsNotVerifiedAndAWarningIsLogged() {
    configuration.configure(new SSLConfigurableSocket(socket));

    verify(socket, never()).setSSLParameters(any(SSLParameters.class));
    assertEquals(Status.WARN, new StatusUtil(context).getHighestLevel(0));
  }
}
