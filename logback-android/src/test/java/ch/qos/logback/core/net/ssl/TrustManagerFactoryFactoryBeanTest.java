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

import static org.junit.Assert.assertNotNull;

import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

import ch.qos.logback.core.net.ssl.TrustManagerFactoryFactoryBean;


/**
 * Unit tests for {@link TrustManagerFactoryFactoryBean}.
 *
 * @author Carl Harris
 */
public class TrustManagerFactoryFactoryBeanTest {

  private TrustManagerFactoryFactoryBean factoryBean =
      new TrustManagerFactoryFactoryBean();

  @Test
  public void testDefaults() throws Exception {
    assertNotNull(factoryBean.createTrustManagerFactory());
  }

  @Test
  public void testExplicitAlgorithm() throws Exception {
    factoryBean.setAlgorithm(TrustManagerFactory.getDefaultAlgorithm());
    assertNotNull(factoryBean.createTrustManagerFactory());
  }

  @Test
  public void testExplicitProvider() throws Exception {
    TrustManagerFactory factory = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    factoryBean.setProvider(factory.getProvider().getName());
    assertNotNull(factoryBean.createTrustManagerFactory());
  }

}
