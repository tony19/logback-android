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

/**
 * An object that has configurable SSL parameters.
 * <p>
 * This interface allows us o decouple the {@link SSLParametersConfiguration}
 * from {@link javax.net.ssl.SSLSocket} and {@link javax.net.ssl.SSLServerSocket}
 * to facilitate unit testing.
 *
 * @author Carl Harris
 * @author Bruno Harbulot
 */
public interface SSLConfigurable {

  /**
   * Gets the set of protocols that the SSL component enables by default.
   *
   * @return protocols (generally a subset of the set returned by
   *    {@link #getSupportedProtocols()}); the return value may be
   *    an empty array but must never be {@code null}.
   */
  String[] getDefaultProtocols();

  /**
   * Gets the set of protocols that the SSL component supports.
   * @return protocols supported protocols; the return value may be
   *    an empty array but must never be {@code null}.
   */
  String[] getSupportedProtocols();

  /**
   * Sets the enabled protocols on the SSL component.
   * @param protocols the protocols to enable
   */
  void setEnabledProtocols(String[] protocols);

  /**
   * Gets the set of cipher suites that the SSL component enables by default.
   *
   * @return cipher suites (generally a subset of the set returned by
   *    {@link #getSupportedCipherSuites()}); the return value may be
   *    an empty array but must never be {@code null}
   */
  String[] getDefaultCipherSuites();

  /**
   * Gets the set of cipher suites that the SSL component supports.
   * @return supported cipher suites; the return value may be
   *    an empty array but must never be {@code null}
   */
  String[] getSupportedCipherSuites();

  /**
   * Sets the enabled cipher suites on the SSL component.
   * @param cipherSuites the cipher suites to enable
   */
  void setEnabledCipherSuites(String[] cipherSuites);

  /**
   * Sets a flag indicating whether the SSL component should require
   * client authentication.
   * @param state the flag state to set
   */
  void setNeedClientAuth(boolean state);

  /**
   * Sets a flag indicating whether the SSL component should request
   * client authentication.
   * @param state the flag state to set
   */
  void setWantClientAuth(boolean state);

  void setHostnameVerification(boolean verifyHostname);
}
