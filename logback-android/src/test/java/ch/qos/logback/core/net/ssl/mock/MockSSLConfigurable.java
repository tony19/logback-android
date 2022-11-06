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
package ch.qos.logback.core.net.ssl.mock;

import ch.qos.logback.core.net.ssl.SSLConfigurable;

public class MockSSLConfigurable implements SSLConfigurable {

  private static final String[] EMPTY = new String[0];

  private String[] defaultProtocols = EMPTY;
  private String[] supportedProtocols = EMPTY;
  private String[] enabledProtocols = EMPTY;
  private String[] defaultCipherSuites = EMPTY;
  private String[] supportedCipherSuites = EMPTY;
  private String[] enabledCipherSuites = EMPTY;
  private boolean needClientAuth;
  private boolean wantClientAuth;

  public String[] getDefaultProtocols() {
    return defaultProtocols;
  }

  public void setDefaultProtocols(String[] defaultProtocols) {
    this.defaultProtocols = defaultProtocols;
  }

  public String[] getSupportedProtocols() {
    return supportedProtocols;
  }

  public void setSupportedProtocols(String[] supportedProtocols) {
    this.supportedProtocols = supportedProtocols;
  }

  public String[] getEnabledProtocols() {
    return enabledProtocols;
  }

  public void setEnabledProtocols(String[] enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  public String[] getDefaultCipherSuites() {
    return defaultCipherSuites;
  }

  public void setDefaultCipherSuites(String[] defaultCipherSuites) {
    this.defaultCipherSuites = defaultCipherSuites;
  }

  public String[] getSupportedCipherSuites() {
    return supportedCipherSuites;
  }

  public void setSupportedCipherSuites(String[] supportedCipherSuites) {
    this.supportedCipherSuites = supportedCipherSuites;
  }

  public String[] getEnabledCipherSuites() {
    return enabledCipherSuites;
  }

  public void setEnabledCipherSuites(String[] enabledCipherSuites) {
    this.enabledCipherSuites = enabledCipherSuites;
  }

  public boolean isNeedClientAuth() {
    return needClientAuth;
  }

  public void setNeedClientAuth(boolean needClientAuth) {
    this.needClientAuth = needClientAuth;
  }

  public boolean isWantClientAuth() {
    return wantClientAuth;
  }

  public void setWantClientAuth(boolean wantClientAuth) {
    this.wantClientAuth = wantClientAuth;
  }

  @Override
  public void setHostnameVerification(boolean verifyHostname) {
  }
}
