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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLSocket;

import android.os.Build;

import org.junit.Test;

/**
 * Unit tests for {@link SSLConfigurableSocket}.
 * <p>
 * The API 24+ hostname verification is covered by the Robolectric
 * {@link SSLHostnameVerificationTest}.
 */
public class SSLConfigurableSocketTest {

  private static final String[] PROTOCOLS = { "TLSv1.2", "TLSv1.3" };
  private static final String[] CIPHER_SUITES = { "SUITE_A", "SUITE_B" };

  private final SSLSocket socket = mock(SSLSocket.class);
  private final SSLConfigurableSocket configurable =
      new SSLConfigurableSocket(socket);

  @Test
  public void defaultProtocolsAreTheSocketsEnabledProtocols() {
    when(socket.getEnabledProtocols()).thenReturn(PROTOCOLS);
    assertArrayEquals(PROTOCOLS, configurable.getDefaultProtocols());
  }

  @Test
  public void supportedProtocolsAreTheSocketsSupportedProtocols() {
    when(socket.getSupportedProtocols()).thenReturn(PROTOCOLS);
    assertArrayEquals(PROTOCOLS, configurable.getSupportedProtocols());
  }

  @Test
  public void enabledProtocolsAreSetOnTheSocket() {
    configurable.setEnabledProtocols(PROTOCOLS);
    verify(socket).setEnabledProtocols(PROTOCOLS);
  }

  @Test
  public void defaultCipherSuitesAreTheSocketsEnabledCipherSuites() {
    when(socket.getEnabledCipherSuites()).thenReturn(CIPHER_SUITES);
    assertArrayEquals(CIPHER_SUITES, configurable.getDefaultCipherSuites());
  }

  @Test
  public void supportedCipherSuitesAreTheSocketsSupportedCipherSuites() {
    when(socket.getSupportedCipherSuites()).thenReturn(CIPHER_SUITES);
    assertArrayEquals(CIPHER_SUITES, configurable.getSupportedCipherSuites());
  }

  @Test
  public void enabledCipherSuitesAreSetOnTheSocket() {
    configurable.setEnabledCipherSuites(CIPHER_SUITES);
    verify(socket).setEnabledCipherSuites(CIPHER_SUITES);
  }

  @Test
  public void needClientAuthIsSetOnTheSocket() {
    configurable.setNeedClientAuth(true);
    verify(socket).setNeedClientAuth(true);
  }

  @Test
  public void wantClientAuthIsSetOnTheSocket() {
    configurable.setWantClientAuth(true);
    verify(socket).setWantClientAuth(true);
  }

  @Test
  public void disabledHostnameVerificationLeavesTheSocketAlone() {
    configurable.setHostnameVerification(false);
    verifyNoInteractions(socket);
  }

  @Test
  public void hostnameVerificationLeavesTheSocketAloneBelowApi24() {
    // plain JVM unit tests see the mockable android.jar's SDK_INT of 0
    assertTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.N);

    configurable.setHostnameVerification(true);

    verifyNoInteractions(socket);
  }

}
