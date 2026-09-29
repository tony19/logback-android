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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Enumeration;

import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

import ch.qos.logback.core.util.LocationUtil;

/**
 * Unit tests for {@link KeyStoreFactoryBean}.
 *
 * @author Carl Harris
 */
public class KeyStoreFactoryBeanTest {

  private static final String FAILING_PROVIDER_NAME =
      "KeyStoreFactoryBeanTestProvider";

  private static final String FAILING_KEYSTORE_TYPE = "NoIntegrityAlgorithm";

  private KeyStoreFactoryBean factoryBean = new KeyStoreFactoryBean();

  @After
  public void tearDown() {
    Security.removeProvider(FAILING_PROVIDER_NAME);
  }

  @Test
  public void testDefaults() throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    assertNotNull(factoryBean.createKeyStore());
  }

  @Test
  public void testExplicitProvider() throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    KeyStore keyStore = factoryBean.createKeyStore();
    factoryBean.setProvider(keyStore.getProvider().getName());
    assertNotNull(factoryBean.createKeyStore());
  }

  @Test
  public void testExplicitType() throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setType(SSL.DEFAULT_KEYSTORE_TYPE);
    assertNotNull(factoryBean.createKeyStore());
  }

  @Test
  public void testPKCS12Type() throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_PKCS12_RESOURCE);
    factoryBean.setType(SSLTestConstants.PKCS12_TYPE);
    assertNotNull(factoryBean.createKeyStore());
  }

  @Test
  public void testExplicitPassphrase() throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setPassword(SSL.DEFAULT_KEYSTORE_PASSWORD);
    assertNotNull(factoryBean.createKeyStore());
  }

  @Test
  public void typeAndPasswordHaveDefaultsAndProviderHasNone() {
    assertNull(factoryBean.getLocation());
    assertEquals(SSL.DEFAULT_KEYSTORE_TYPE, factoryBean.getType());
    assertEquals(SSL.DEFAULT_KEYSTORE_PASSWORD, factoryBean.getPassword());
    assertNull(factoryBean.getProvider());
  }

  @Test
  public void configuredPropertiesReplaceTheDefaults() {
    factoryBean.setType(SSLTestConstants.PKCS12_TYPE);
    factoryBean.setPassword("secret");
    factoryBean.setProvider("SomeProvider");

    assertEquals(SSLTestConstants.PKCS12_TYPE, factoryBean.getType());
    assertEquals("secret", factoryBean.getPassword());
    assertEquals("SomeProvider", factoryBean.getProvider());
  }

  @Test
  public void keyStoreIsLoadedFromTheConfiguredProviderAndType()
      throws Exception {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_PKCS12_RESOURCE);
    factoryBean.setType(SSLTestConstants.PKCS12_TYPE);
    String provider = KeyStore.getInstance(SSLTestConstants.PKCS12_TYPE)
        .getProvider().getName();
    factoryBean.setProvider(provider);

    KeyStore keyStore = factoryBean.createKeyStore();

    assertEquals(SSLTestConstants.PKCS12_TYPE, keyStore.getType());
    assertEquals(provider, keyStore.getProvider().getName());
    assertTrue(keyStore.size() > 0);
  }

  @Test
  public void locationIsRequired() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> factoryBean.createKeyStore());
    assertEquals("location is required", ex.getMessage());
  }

  @Test
  public void unknownProviderIsReported() {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setProvider(SSLTestConstants.FAKE_PROVIDER_NAME);

    NoSuchProviderException ex = assertThrows(NoSuchProviderException.class,
        () -> factoryBean.createKeyStore());

    assertEquals("no such keystore provider: "
        + SSLTestConstants.FAKE_PROVIDER_NAME, ex.getMessage());
  }

  @Test
  public void keyStoreThatNeedsAnUnavailableAlgorithmIsReported() {
    Security.addProvider(new FailingKeyStoreProvider());
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setProvider(FAILING_PROVIDER_NAME);
    factoryBean.setType(FAILING_KEYSTORE_TYPE);

    NoSuchAlgorithmException ex = assertThrows(NoSuchAlgorithmException.class,
        () -> factoryBean.createKeyStore());

    assertEquals("no such keystore type: " + FAILING_KEYSTORE_TYPE,
        ex.getMessage());
  }

  @Test
  public void missingKeyStoreIsReportedAsFileNotFound() {
    String location = "classpath:net/ssl/no-such-keystore.jks";
    factoryBean.setLocation(location);

    KeyStoreException ex = assertThrows(KeyStoreException.class,
        () -> factoryBean.createKeyStore());

    assertEquals(location + ": file not found", ex.getMessage());
  }

  @Test
  public void unknownTypeIsReportedWithItsCause() {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setType(SSLTestConstants.FAKE_ALGORITHM_NAME);

    KeyStoreException ex = assertThrows(KeyStoreException.class,
        () -> factoryBean.createKeyStore());

    assertTrue(ex.getCause() instanceof KeyStoreException);
    assertEquals(SSLTestConstants.KEYSTORE_JKS_RESOURCE + ": "
        + ex.getCause().getMessage(), ex.getMessage());
  }

  @Test
  public void wrongPasswordIsReportedWithItsCause() {
    factoryBean.setLocation(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    factoryBean.setPassword("not the password");

    KeyStoreException ex = assertThrows(KeyStoreException.class,
        () -> factoryBean.createKeyStore());

    assertTrue(ex.getCause() instanceof IOException);
    assertEquals(SSLTestConstants.KEYSTORE_JKS_RESOURCE + ": "
        + ex.getCause().getMessage(), ex.getMessage());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void failureToCloseTheKeyStoreStreamIsPrintedAndIgnored()
      throws Exception {
    final URL keyStoreUrl =
        LocationUtil.urlForResource(SSLTestConstants.KEYSTORE_JKS_RESOURCE);
    URL unclosableUrl = new URL("test", "", -1, "keystore.jks",
        new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {
              @Override
              public void connect() {
              }

              @Override
              public InputStream getInputStream() throws IOException {
                return new FilterInputStream(keyStoreUrl.openStream()) {
                  @Override
                  public void close() throws IOException {
                    super.close();
                    throw new IOException("close failed");
                  }
                };
              }
            };
          }
        });
    factoryBean.setLocation("test:keystore.jks");
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    PrintStream originalStderr = System.err;
    KeyStore keyStore;
    try (MockedStatic<LocationUtil> locationUtil =
             mockStatic(LocationUtil.class)) {
      locationUtil.when(() -> LocationUtil.urlForResource("test:keystore.jks"))
          .thenReturn(unclosableUrl);
      System.setErr(new PrintStream(stderr, true, "UTF-8"));

      keyStore = factoryBean.createKeyStore();
    }
    finally {
      System.setErr(originalStderr);
    }

    assertTrue(keyStore.size() > 0);
    assertTrue(stderr.toString("UTF-8")
        .contains("java.io.IOException: close failed"));
  }

  /**
   * A JCA provider of a key store type whose integrity check needs an
   * algorithm that is not available.
   */
  private static class FailingKeyStoreProvider extends Provider {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("deprecation")
    FailingKeyStoreProvider() {
      super(FAILING_PROVIDER_NAME, 1.0,
          "key stores for KeyStoreFactoryBeanTest");
      put("KeyStore." + FAILING_KEYSTORE_TYPE,
          NoIntegrityAlgorithmKeyStoreSpi.class.getName());
    }
  }

  /**
   * A key store that cannot be loaded because its integrity check algorithm
   * is not available. Public, so that {@link Provider} can instantiate it.
   */
  public static class NoIntegrityAlgorithmKeyStoreSpi extends KeyStoreSpi {

    @Override
    public void engineLoad(InputStream stream, char[] password)
        throws NoSuchAlgorithmException {
      throw new NoSuchAlgorithmException(
          "integrity check algorithm unavailable");
    }

    @Override
    public Key engineGetKey(String alias, char[] password) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Certificate engineGetCertificate(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Date engineGetCreationDate(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password,
        Certificate[] chain) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key,
        Certificate[] chain) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate cert) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void engineDeleteEntry(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<String> engineAliases() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean engineContainsAlias(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int engineSize() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean engineIsKeyEntry(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean engineIsCertificateEntry(String alias) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String engineGetCertificateAlias(Certificate cert) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) {
      throw new UnsupportedOperationException();
    }
  }

}
