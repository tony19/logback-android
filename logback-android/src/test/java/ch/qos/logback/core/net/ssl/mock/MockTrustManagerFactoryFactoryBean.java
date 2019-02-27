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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.net.ssl.TrustManagerFactory;

import ch.qos.logback.core.net.ssl.TrustManagerFactoryFactoryBean;

/**
 * A {@link TrustManagerFactoryFactoryBean} with test instrumentation.
 *
 * @author Carl Harris
 */
public class MockTrustManagerFactoryFactoryBean
    extends TrustManagerFactoryFactoryBean {

  private boolean factoryCreated;

  @Override
  public TrustManagerFactory createTrustManagerFactory()
      throws NoSuchProviderException, NoSuchAlgorithmException {
    factoryCreated = true;
    return super.createTrustManagerFactory();
  }

  public boolean isFactoryCreated() {
    return factoryCreated;
  }

}
