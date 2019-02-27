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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.net.ssl.KeyManagerFactory;

/**
 * A factory bean for a JSSE {@link KeyManagerFactory}.
 * <p>
 * This object holds the configurable properties of a key manager factory
 * and uses them to create and load a {@link KeyManagerFactory} instance.
 *
 * @author Carl Harris
 */
public class KeyManagerFactoryFactoryBean {

  private String algorithm;
  private String provider;

  /**
   * Creates a {@link KeyManagerFactory} using the receiver's configuration.
   * @return factory object
   * @throws NoSuchProviderException if the provider specified by
   *    {@link #setProvider(String)} is not known to the platform
   * @throws NoSuchAlgorithmException if the algorithm specified by
   *    {@link #setAlgorithm(String)} is not known to the specified provider
   *    (or to the default platform provider if no provider is specified)
   */
  public KeyManagerFactory createKeyManagerFactory()
      throws NoSuchProviderException, NoSuchAlgorithmException {

    return getProvider() != null ?
        KeyManagerFactory.getInstance(getAlgorithm(), getProvider())
        : KeyManagerFactory.getInstance(getAlgorithm());
  }

  /**
   * Gets the algorithm name for the key manager factory.
   * @return algorithm name (e.g. {@code SunX509}); the default algorithm
   *    (obtained from {@link KeyManagerFactory#getDefaultAlgorithm()})
   *    is returned if no algorithm has been configured
   */
  public String getAlgorithm() {
    if (algorithm == null) {
      return KeyManagerFactory.getDefaultAlgorithm();
    }
    return algorithm;
  }

  /**
   * Sets the algorithm name for the key manager factory.
   * @param algorithm an algorithm name, which must be recognized by the
   *    provider specified by {@link #setProvider(String)} or by the
   *    platform's default provider if no provider is specified.
   */
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Gets the JSSE provider name for the key manager factory.
   * @return provider name
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Sets the JSSE provider name for the key manager factory.
   * @param provider name of the JSSE provider to utilize in creating the
   *    key manager factory
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

}
