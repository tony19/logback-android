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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;

/**
 * Unit tests for {@link SSLNestedComponentRegistryRules}.
 */
public class SSLNestedComponentRegistryRulesTest {

  private final DefaultNestedComponentRegistry registry =
      new DefaultNestedComponentRegistry();

  @Test
  public void registersTheDefaultTypesOfTheSslConfigurationElements() {
    SSLNestedComponentRegistryRules.addDefaultNestedComponentRegistryRules(
        registry);

    assertEquals(SSLConfiguration.class,
        registry.findDefaultComponentType(SSLComponent.class, "ssl"));
    assertEquals(SSLParametersConfiguration.class,
        registry.findDefaultComponentType(SSLConfiguration.class,
            "parameters"));
    assertEquals(KeyStoreFactoryBean.class,
        registry.findDefaultComponentType(SSLConfiguration.class, "keyStore"));
    assertEquals(KeyStoreFactoryBean.class,
        registry.findDefaultComponentType(SSLConfiguration.class,
            "trustStore"));
    assertEquals(KeyManagerFactoryFactoryBean.class,
        registry.findDefaultComponentType(SSLConfiguration.class,
            "keyManagerFactory"));
    assertEquals(TrustManagerFactoryFactoryBean.class,
        registry.findDefaultComponentType(SSLConfiguration.class,
            "trustManagerFactory"));
    assertEquals(SecureRandomFactoryBean.class,
        registry.findDefaultComponentType(SSLConfiguration.class,
            "secureRandom"));
  }

  @Test
  public void rulesClassHasAPublicDefaultConstructor() {
    // part of the public API, although all of its members are static
    assertNotNull(new SSLNestedComponentRegistryRules());
  }

}
