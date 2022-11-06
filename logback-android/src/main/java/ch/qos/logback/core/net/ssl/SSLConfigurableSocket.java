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

import android.annotation.TargetApi;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * An {@link SSLConfigurable} wrapper for an {@link SSLSocket}.
 *
 * @author Carl Harris
 * @author Bruno Harbulot
 */
public class SSLConfigurableSocket implements SSLConfigurable {

  private final SSLSocket delegate;

  public SSLConfigurableSocket(SSLSocket delegate) {
    this.delegate = delegate;
  }

  public String[] getDefaultProtocols() {
    return delegate.getEnabledProtocols();
  }

  public String[] getSupportedProtocols() {
    return delegate.getSupportedProtocols();
  }

  public void setEnabledProtocols(String[] protocols) {
    delegate.setEnabledProtocols(protocols);
  }

  public String[] getDefaultCipherSuites() {
    return delegate.getEnabledCipherSuites();
  }

  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  public void setEnabledCipherSuites(String[] suites) {
    delegate.setEnabledCipherSuites(suites);
  }

  public void setNeedClientAuth(boolean state) {
    delegate.setNeedClientAuth(state);
  }

  public void setWantClientAuth(boolean state) {
    delegate.setWantClientAuth(state);
  }

  @TargetApi(24)
  @Override
  public void setHostnameVerification(boolean hostnameVerification) {
    if (!hostnameVerification) {
      return;
    }
    SSLParameters sslParameters = delegate.getSSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    delegate.setSSLParameters(sslParameters);
  }
}
