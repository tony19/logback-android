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

import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;

/**
 * Nested component registry rules for {@link SSLConfiguration} and its
 * components.
 *
 * @author Carl Harris
 */
public class SSLNestedComponentRegistryRules {

  static public void addDefaultNestedComponentRegistryRules(
      DefaultNestedComponentRegistry registry) {
    registry.add(SSLComponent.class, "ssl", SSLConfiguration.class);
    registry.add(SSLConfiguration.class, "parameters",
        SSLParametersConfiguration.class);
    registry.add(SSLConfiguration.class, "keyStore",
        KeyStoreFactoryBean.class);
    registry.add(SSLConfiguration.class, "trustStore",
        KeyStoreFactoryBean.class);
    registry.add(SSLConfiguration.class, "keyManagerFactory",
        KeyManagerFactoryFactoryBean.class);
    registry.add(SSLConfiguration.class, "trustManagerFactory",
        TrustManagerFactoryFactoryBean.class);
    registry.add(SSLConfiguration.class, "secureRandom",
        SecureRandomFactoryBean.class);
  }

}
