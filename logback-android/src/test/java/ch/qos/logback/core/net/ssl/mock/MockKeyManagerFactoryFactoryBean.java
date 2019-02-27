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

import javax.net.ssl.KeyManagerFactory;

import ch.qos.logback.core.net.ssl.KeyManagerFactoryFactoryBean;

/**
 * A {@link KeyManagerFactoryFactoryBean} with test instrumentation.
 *
 * @author Carl Harris
 */
public class MockKeyManagerFactoryFactoryBean
    extends KeyManagerFactoryFactoryBean {

  private boolean factoryCreated;

  @Override
  public KeyManagerFactory createKeyManagerFactory()
      throws NoSuchProviderException, NoSuchAlgorithmException {
    factoryCreated = true;
    return super.createKeyManagerFactory();
  }

  public boolean isFactoryCreated() {
    return factoryCreated;
  }

}
