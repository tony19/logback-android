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
package ch.qos.logback.classic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Loader;

/**
 * Plain-JVM tests (no Robolectric) of the configuration search done by
 * {@link ContextInitializer#autoConfig()}: first the file, URL or resource
 * named by the {@code logback.configurationFile} system property, then
 * {@code assets/logback.xml} from the initializer's class loader.
 *
 * <p>The class loader is injected so that the presence of
 * {@code assets/logback.xml} does not depend on the test classpath, and the
 * {@link JoranConfigurator} is mocked (its XML parser is Android's) so the
 * tests pin which URL the search hands to it.
 */
public class ContextInitializerJvmTest {

  private static final String ASSET_STATUS_REGEX = ".*\\[" + ContextInitializer.AUTOCONFIG_FILE + "\\].*";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private final LoggerContext loggerContext = new LoggerContext();
  private String savedConfigFileProperty;
  private URLClassLoader apkClassLoader;
  private File apk;

  @Before
  public void setUp() throws IOException {
    savedConfigFileProperty = System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    apk = tmp.newFolder("apk");
  }

  @After
  public void tearDown() throws IOException {
    if (savedConfigFileProperty == null) {
      System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    } else {
      System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, savedConfigFileProperty);
    }
    if (apkClassLoader != null) {
      apkClassLoader.close();
    }
    loggerContext.stop();
  }

  @Test
  public void configuresFromAssetsWhenSystemPropertyIsNotSet() throws Exception {
    File asset = writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(asset.toURI().toURL()), configured);
    assertEquals(Status.INFO, statusLevel("Found resource [" + ContextInitializer.AUTOCONFIG_FILE
        + "] at [" + ContextInitializer.AUTOCONFIG_FILE + "]"));
  }

  @Test
  public void reportsMissingAssetAndConfiguresNothing() throws Exception {
    List<URL> configured = autoConfig();

    assertEquals(Collections.<URL>emptyList(), configured);
    assertEquals(Status.INFO, statusLevel("Could NOT find resource [" + ContextInitializer.AUTOCONFIG_FILE + "]"));
  }

  @Test
  public void configFileFromSystemPropertyWinsOverAssets() throws Exception {
    writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    File configFile = writeFile(new File(tmp.getRoot(), "fromProperty.xml"));
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(configFile.toURI().toURL()), configured);
    assertEquals(Status.INFO, statusLevel("Found resource [" + configFile.getAbsolutePath()
        + "] at [" + configFile.getAbsolutePath() + "]"));
    assertEquals(Status.INFO, statusLevel("Found resource [" + configFile.getAbsolutePath()
        + "] at [" + configFile.toURI().toURL() + "]"));
    // the assets are not searched once the system property found a config
    assertEquals(0, matchingStatuses(ASSET_STATUS_REGEX).size());
  }

  @Test
  public void failureToConfigureFromSystemPropertyIsNotMaskedByAssets() throws Exception {
    writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    File configFile = writeFile(new File(tmp.getRoot(), "broken.xml"));
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
    final JoranException failure = new JoranException("broken config");
    final ContextInitializer initializer = initializerLoadingFromApk();

    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
        (mock, context) -> doThrow(failure).when(mock).doConfigure(any(URL.class)))) {
      JoranException thrown = assertThrows(JoranException.class, initializer::autoConfig);
      assertSame(failure, thrown);
      verify(configurators.constructed().get(0)).doConfigure(configFile.toURI().toURL());
    }
    assertEquals(0, matchingStatuses(ASSET_STATUS_REGEX).size());
  }

  @Test
  public void urlNamedBySystemPropertyIsConfigured() throws Exception {
    writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    URL url = URI.create("http://localhost/logback-config.xml").toURL();
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, url.toString());

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(url), configured);
    assertEquals(Status.INFO, statusLevel("Found resource [" + url + "] at [" + url + "]"));
    assertEquals(0, matchingStatuses(ASSET_STATUS_REGEX).size());
  }

  @Test
  public void directoryNamedBySystemPropertyIsNotAConfigFileSoAssetsAreSearched() throws Exception {
    File asset = writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    File directory = tmp.newFolder("configDir");
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, directory.getAbsolutePath());

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(asset.toURI().toURL()), configured);
    assertEquals(Status.INFO, statusLevel("Could NOT find resource [" + directory.getAbsolutePath() + "]"));
    assertEquals(0, matchingStatuses("Found resource \\[" + Pattern.quote(directory.getAbsolutePath())
        + "\\].*").size());
  }

  @Test
  public void unknownResourceNamedBySystemPropertyFallsBackToAssets() throws Exception {
    File asset = writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "no-such-logback-config.xml");

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(asset.toURI().toURL()), configured);
    assertEquals(Status.INFO, statusLevel("Could NOT find resource [no-such-logback-config.xml]"));
  }

  @Test
  public void resourceNamedBySystemPropertyIsLoadedFromClassLoader() throws Exception {
    writeFile(new File(apk, ContextInitializer.AUTOCONFIG_FILE));
    File resource = writeFile(new File(apk, "custom-logback.xml"));
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "custom-logback.xml");

    List<URL> configured = autoConfig();

    assertEquals(Collections.singletonList(resource.toURI().toURL()), configured);
    assertEquals(Status.INFO, statusLevel("Found resource [custom-logback.xml] at ["
        + resource.toURI().toURL() + "]"));
    assertEquals(0, matchingStatuses(ASSET_STATUS_REGEX).size());
  }

  /**
   * Runs {@link ContextInitializer#autoConfig()} over the {@code apk} folder
   * and returns the URLs it handed to the configurator, in order.
   */
  private List<URL> autoConfig() throws Exception {
    ContextInitializer initializer = initializerLoadingFromApk();
    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class)) {
      initializer.autoConfig();

      assertEquals(1, configurators.constructed().size());
      JoranConfigurator configurator = configurators.constructed().get(0);
      verify(configurator).setContext(loggerContext);
      ArgumentCaptor<URL> urls = ArgumentCaptor.forClass(URL.class);
      verify(configurator, atLeast(0)).doConfigure(urls.capture());
      return urls.getAllValues();
    }
  }

  /**
   * Creates a {@link ContextInitializer} whose class loader sees only the
   * contents of the {@code apk} folder, standing in for an app's APK.
   */
  private ContextInitializer initializerLoadingFromApk() throws IOException {
    apkClassLoader = new URLClassLoader(new URL[] { apk.toURI().toURL() }, null);
    try (MockedStatic<Loader> loader = mockStatic(Loader.class)) {
      loader.when(() -> Loader.getClassLoaderOfObject(any())).thenReturn(apkClassLoader);
      return new ContextInitializer(loggerContext);
    }
  }

  private static File writeFile(File file) throws IOException {
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent);
    }
    try (OutputStream out = new FileOutputStream(file)) {
      out.write("<configuration/>".getBytes("UTF-8"));
    }
    return file;
  }

  /** Level of the one status whose message is exactly {@code message}. */
  private int statusLevel(String message) {
    List<Status> found = new ArrayList<Status>();
    for (Status s : loggerContext.getStatusManager().getCopyOfStatusList()) {
      if (s.getMessage().equals(message)) {
        found.add(s);
      }
    }
    assertEquals("statuses with message [" + message + "]: " + found, 1, found.size());
    return found.get(0).getLevel();
  }

  private List<Status> matchingStatuses(String regex) {
    List<Status> found = new ArrayList<Status>();
    for (Status s : loggerContext.getStatusManager().getCopyOfStatusList()) {
      if (s.getMessage().matches(regex)) {
        found.add(s);
      }
    }
    return found;
  }
}
