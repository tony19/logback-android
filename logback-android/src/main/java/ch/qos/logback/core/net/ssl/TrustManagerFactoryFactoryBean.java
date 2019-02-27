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

import javax.net.ssl.TrustManagerFactory;

/**
 * A factory bean for a JSSE {@link TrustManagerFactory}.
 * <p>
 * This object holds the configurable properties of a trust manager factory
 * and uses them to create and load a {@link TrustManagerFactory} instance.
 *
 * @author Carl Harris
 */
public class TrustManagerFactoryFactoryBean {

  private String algorithm;
  private String provider;

  /**
   * Creates a {@link TrustManagerFactory} using the receiver's configuration.
   * @return factory object
   * @throws NoSuchProviderException if the provider specified by
   *    {@link #setProvider(String)} is not known to the platform
   * @throws NoSuchAlgorithmException if the algorithm specified by
   *    {@link #setAlgorithm(String)} is not known to the specified provider
   *    (or to the default platform provider if no provider is specified)
   */
  public TrustManagerFactory createTrustManagerFactory()
      throws NoSuchProviderException, NoSuchAlgorithmException {

    return getProvider() != null ?
        TrustManagerFactory.getInstance(getAlgorithm(), getProvider())
        : TrustManagerFactory.getInstance(getAlgorithm());
  }

  /**
   * Gets the algorithm name for the trust manager factory.
   * @return algorithm name (e.g. {@code PKIX}); the default algorithm
   *    (obtained from {@link TrustManagerFactory#getDefaultAlgorithm()})
   *    is returned if no algorithm has been configured
   */
  public String getAlgorithm() {
    if (algorithm == null) {
      return TrustManagerFactory.getDefaultAlgorithm();
    }
    return algorithm;
  }

  /**
   * Sets the algorithm name for the trust manager factory.
   * @param algorithm an algorithm name, which must be recognized by the
   *    provider specified by {@link #setProvider(String)} or by the
   *    platform's default provider if no provider is specified.
   */
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Gets the JSSE provider name for the trust manager factory.
   * @return provider name
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Sets the JSSE provider name for the trust manager factory.
   * @param provider name of the JSSE provider to utilize in creating the
   *    trust manager factory
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }

}
