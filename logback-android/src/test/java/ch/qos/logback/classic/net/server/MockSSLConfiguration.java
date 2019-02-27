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
package ch.qos.logback.classic.net.server;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.spi.ContextAware;

/**
 * A mock {@link SSLConfiguration} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
class MockSSLConfiguration extends SSLConfiguration {

  private boolean contextCreated;

  @Override
  public SSLContext createContext(ContextAware context)
      throws NoSuchProviderException, NoSuchAlgorithmException,
      KeyManagementException, UnrecoverableKeyException, KeyStoreException,
      CertificateException {
    contextCreated = true;
    return super.createContext(context);
  }

  public boolean isContextCreated() {
    return contextCreated;
  }

}
