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
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

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
  public void socketFactoryVerifiesHostnameByDefault() throws IOException {
    Socket created = newSocketFactory().createSocket(InetAddress.getLoopbackAddress(), 4560);

    assertSame(socket, created);
    ArgumentCaptor<SSLParameters> captor = ArgumentCaptor.forClass(SSLParameters.class);
    verify(socket).setSSLParameters(captor.capture());
    assertEquals("HTTPS", captor.getValue().getEndpointIdentificationAlgorithm());
  }

  @Test
  @Config(sdk = 29)
  public void socketFactoryHostnameVerificationCanBeDisabled() throws IOException {
    configuration.setHostnameVerification(false);
    Socket created = newSocketFactory().createSocket("localhost", 4560);

    assertSame(socket, created);
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
  @Config(sdk = 29)
  public void belowApi24HostnameIsNotVerifiedAndAWarningIsLogged() {
    // the jdk11 flavor's minSdk is 26, so Robolectric can't run at sdk 23;
    // fake the API level instead
    int sdkInt = Build.VERSION.SDK_INT;
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 23);
    try {
      configuration.configure(new SSLConfigurableSocket(socket));
    } finally {
      ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", sdkInt);
    }

    verify(socket, never()).setSSLParameters(any(SSLParameters.class));
    assertEquals(Status.WARN, new StatusUtil(context).getHighestLevel(0));
  }

  private ConfigurableSSLSocketFactory newSocketFactory() throws IOException {
    SSLSocketFactory delegate = mock(SSLSocketFactory.class);
    when(delegate.createSocket(any(InetAddress.class), anyInt())).thenReturn(socket);
    when(delegate.createSocket(any(String.class), anyInt())).thenReturn(socket);
    return new ConfigurableSSLSocketFactory(configuration, delegate);
  }
}
