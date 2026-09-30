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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.ssl.mock.MockContextAware;

/**
 * Plain JVM unit tests for the configuration properties of
 * {@link SSLContextFactoryBean}, including those it takes from the JSSE
 * system properties. {@link SSLContextFactoryBeanTest} covers creating a
 * context from configured components.
 */
public class SSLContextFactoryBeanConfigurationTest {

  private static final String KEY_STORE = "javax.net.ssl.keyStore";
  private static final String TRUST_STORE = "javax.net.ssl.trustStore";
  private static final String[] JSSE_PROPERTIES = {
      KEY_STORE, KEY_STORE + "Provider", KEY_STORE + "Password",
      KEY_STORE + "Type",
      TRUST_STORE, TRUST_STORE + "Provider", TRUST_STORE + "Password",
      TRUST_STORE + "Type",
  };

  private final Map<String, String> savedProperties =
      new HashMap<String, String>();

  private final MockContextAware context = new MockContextAware();
  private final SSLContextFactoryBean factoryBean = new SSLContextFactoryBean();

  @Before
  public void setUp() {
    for (String name : JSSE_PROPERTIES) {
      savedProperties.put(name, System.getProperty(name));
      System.clearProperty(name);
    }
  }

  @After
  public void tearDown() {
    for (Map.Entry<String, String> property : savedProperties.entrySet()) {
      if (property.getValue() == null) {
        System.clearProperty(property.getKey());
      } else {
        System.setProperty(property.getKey(), property.getValue());
      }
    }
  }

  @Test
  public void protocolDefaultsToSslAndCanBeChanged() {
    assertEquals(SSL.DEFAULT_PROTOCOL, factoryBean.getProtocol());

    factoryBean.setProtocol("TLS");

    assertEquals("TLS", factoryBean.getProtocol());
  }

  @Test
  public void providerIsUnsetByDefaultAndCanBeChanged() {
    assertNull(factoryBean.getProvider());

    factoryBean.setProvider("SomeProvider");

    assertEquals("SomeProvider", factoryBean.getProvider());
  }

  @Test
  public void contextIsCreatedForTheConfiguredProtocolAndProvider()
      throws Exception {
    String provider = SSLContext.getInstance("TLS").getProvider().getName();
    factoryBean.setProtocol("TLS");
    factoryBean.setProvider(provider);

    SSLContext sslContext = factoryBean.createContext(context);

    assertEquals("TLS", sslContext.getProtocol());
    assertEquals(provider, sslContext.getProvider().getName());
    assertTrue(context.hasInfoMatching(
        "SSL protocol 'TLS' provider '" + Pattern.quote(provider) + ".*'"));
  }

  @Test
  public void unknownProviderIsReported() {
    factoryBean.setProvider(SSLTestConstants.FAKE_PROVIDER_NAME);

    assertThrows(NoSuchProviderException.class,
        () -> factoryBean.createContext(context));
  }

  @Test
  public void defaultManagerFactoriesUseThePlatformDefaultAlgorithms() {
    assertEquals(KeyManagerFactory.getDefaultAlgorithm(),
        factoryBean.getKeyManagerFactory().getAlgorithm());
    assertEquals(TrustManagerFactory.getDefaultAlgorithm(),
        factoryBean.getTrustManagerFactory().getAlgorithm());
  }

  @Test
  public void configuredManagerFactoriesReplaceTheDefaults() {
    KeyManagerFactoryFactoryBean keyManagerFactory =
        new KeyManagerFactoryFactoryBean();
    TrustManagerFactoryFactoryBean trustManagerFactory =
        new TrustManagerFactoryFactoryBean();

    factoryBean.setKeyManagerFactory(keyManagerFactory);
    factoryBean.setTrustManagerFactory(trustManagerFactory);

    assertSame(keyManagerFactory, factoryBean.getKeyManagerFactory());
    assertSame(trustManagerFactory, factoryBean.getTrustManagerFactory());
  }

  @Test
  public void configuredStoresAreUsedWithTheDefaultManagerFactories()
      throws Exception {
    KeyStoreFactoryBean keyStore = new KeyStoreFactoryBean();
    keyStore.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    KeyStoreFactoryBean trustStore = new KeyStoreFactoryBean();
    trustStore.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setKeyStore(keyStore);
    factoryBean.setTrustStore(trustStore);

    assertNotNull(factoryBean.createContext(context));

    assertTrue(context.hasInfoMatching("key manager algorithm '"
        + Pattern.quote(KeyManagerFactory.getDefaultAlgorithm()) + "'.*"));
    assertTrue(context.hasInfoMatching("trust manager algorithm '"
        + Pattern.quote(TrustManagerFactory.getDefaultAlgorithm()) + "'.*"));
  }

  @Test
  public void storesAreUnsetWithoutJsseSystemProperties() {
    assertNull(factoryBean.getKeyStore());
    assertNull(factoryBean.getTrustStore());
  }

  @Test
  public void keyStoreIsTakenFromTheJsseSystemProperties() {
    System.setProperty(KEY_STORE, "/data/keystore.p12");
    System.setProperty(KEY_STORE + "Provider", "KeyProvider");
    System.setProperty(KEY_STORE + "Password", "key secret");
    System.setProperty(KEY_STORE + "Type", "PKCS12");

    KeyStoreFactoryBean keyStore = factoryBean.getKeyStore();

    assertEquals("file:/data/keystore.p12", keyStore.getLocation());
    assertEquals("KeyProvider", keyStore.getProvider());
    assertEquals("key secret", keyStore.getPassword());
    assertEquals("PKCS12", keyStore.getType());
    assertSame(keyStore, factoryBean.getKeyStore());
    assertNull(factoryBean.getTrustStore());
  }

  @Test
  public void trustStoreIsTakenFromTheJsseSystemProperties() {
    System.setProperty(TRUST_STORE, "/data/truststore.jks");
    System.setProperty(TRUST_STORE + "Provider", "TrustProvider");
    System.setProperty(TRUST_STORE + "Password", "trust secret");
    System.setProperty(TRUST_STORE + "Type", "JKS");

    KeyStoreFactoryBean trustStore = factoryBean.getTrustStore();

    assertEquals("file:/data/truststore.jks", trustStore.getLocation());
    assertEquals("TrustProvider", trustStore.getProvider());
    assertEquals("trust secret", trustStore.getPassword());
    assertEquals("JKS", trustStore.getType());
    assertSame(trustStore, factoryBean.getTrustStore());
    assertNull(factoryBean.getKeyStore());
  }

  @Test
  public void storeFromJsseSystemPropertiesUsesDefaultsForUnsetProperties() {
    System.setProperty(KEY_STORE, "/data/keystore.jks");

    KeyStoreFactoryBean keyStore = factoryBean.getKeyStore();

    assertNull(keyStore.getProvider());
    assertEquals(SSL.DEFAULT_KEYSTORE_PASSWORD, keyStore.getPassword());
    assertEquals(SSL.DEFAULT_KEYSTORE_TYPE, keyStore.getType());
  }

  @Test
  public void storeLocationThatIsAlreadyAFileUrlIsKept() {
    System.setProperty(TRUST_STORE, "file:/data/truststore.jks");

    assertEquals("file:/data/truststore.jks",
        factoryBean.getTrustStore().getLocation());
  }

  @Test
  public void locationOfAnUnsetSystemPropertyIsNull() throws Exception {
    // getKeyStore() and getTrustStore() only ask for the location of a
    // property that is set, so this can only be reached directly
    Method locationFromSystemProperty = SSLContextFactoryBean.class
        .getDeclaredMethod("locationFromSystemProperty", String.class);
    locationFromSystemProperty.setAccessible(true);

    assertNull(locationFromSystemProperty.invoke(factoryBean, KEY_STORE));
  }

}
