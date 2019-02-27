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

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import ch.qos.logback.core.net.ssl.ConfigurableSSLSocketFactory;
import ch.qos.logback.core.net.ssl.SSLComponent;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;

/**
 * A {@link SocketReceiver} that supports SSL.
 *
 * @author Carl Harris
 */
public class SSLSocketReceiver extends SocketReceiver
    implements SSLComponent {

  private SSLConfiguration ssl;
  private SocketFactory socketFactory;

  /**
   * Gets an {@link SocketFactory} that produces SSL sockets using an
   * {@link SSLContext} that is derived from the receiver's configuration.
   * @return socket factory
   */
  @Override
  protected SocketFactory getSocketFactory() {
    return socketFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean shouldStart() {
    try {
      SSLContext sslContext = getSsl().createContext(this);
      SSLParametersConfiguration parameters = getSsl().getParameters();
      parameters.setContext(getContext());
      socketFactory = new ConfigurableSSLSocketFactory(parameters,
          sslContext.getSocketFactory());
      return super.shouldStart();
    }
    catch (Exception ex) {
      addError(ex.getMessage(), ex);
      return false;
    }
  }

  /**
   * Gets the SSL configuration.
   * @return SSL configuration; if no configuration has been set, a
   *    default configuration is returned
   */
  public SSLConfiguration getSsl() {
    if (ssl == null) {
      ssl = new SSLConfiguration();
    }
    return ssl;
  }

  /**
   * Sets the SSL configuration.
   * @param ssl the SSL configuration to set
   */
  public void setSsl(SSLConfiguration ssl) {
    this.ssl = ssl;
  }

}
