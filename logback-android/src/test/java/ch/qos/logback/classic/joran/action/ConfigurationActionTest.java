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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.ReconfigureOnChangeTask;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.Duration;

public class ConfigurationActionTest {

  private final LoggerContext context = new LoggerContext();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final ConfigurationAction action = new ConfigurationAction();
  private final AttributesImpl attributes = new AttributesImpl();
  private String savedDebugProperty;

  @Before
  public void setUp() throws Exception {
    savedDebugProperty = System.getProperty(ConfigurationAction.DEBUG_SYSTEM_PROPERTY_KEY);
    System.clearProperty(ConfigurationAction.DEBUG_SYSTEM_PROPERTY_KEY);
    action.setContext(context);
  }

  @After
  public void tearDown() {
    if (savedDebugProperty == null) {
      System.clearProperty(ConfigurationAction.DEBUG_SYSTEM_PROPERTY_KEY);
    } else {
      System.setProperty(ConfigurationAction.DEBUG_SYSTEM_PROPERTY_KEY, savedDebugProperty);
    }
    context.stop();
  }

  @Test
  public void debugSystemPropertyOverridesTheDebugAttribute() {
    System.setProperty(ConfigurationAction.DEBUG_SYSTEM_PROPERTY_KEY, "true");
    addAttribute(ConfigurationAction.INTERNAL_DEBUG_ATTR, "false");

    action.begin(ic, "configuration", attributes);

    assertEquals(1, onConsoleStatusListeners());
    assertEquals(0, statusCount("debug attribute not set"));
  }

  @Test
  public void debugAttributeSetToTheStringNullDoesNotEnableDebug() {
    addAttribute(ConfigurationAction.INTERNAL_DEBUG_ATTR, "null");

    action.begin(ic, "configuration", attributes);

    assertEquals(0, onConsoleStatusListeners());
    assertEquals(1, statusCount("debug attribute not set"));
  }

  @Test
  public void debugAttributeSetToFalseDoesNotEnableDebug() {
    addAttribute(ConfigurationAction.INTERNAL_DEBUG_ATTR, "False");

    action.begin(ic, "configuration", attributes);

    assertEquals(0, onConsoleStatusListeners());
    assertEquals(1, statusCount("debug attribute not set"));
  }

  @Test
  public void debugAttributeSetToTrueEnablesDebug() {
    addAttribute(ConfigurationAction.INTERNAL_DEBUG_ATTR, "true");

    action.begin(ic, "configuration", attributes);

    assertEquals(1, onConsoleStatusListeners());
    assertEquals(0, statusCount("debug attribute not set"));
  }

  @Test
  public void beginPushesTheContextAndEndPopsIt() {
    addAttribute(ConfigurationAction.PACKAGING_DATA_ATTR, "true");

    action.begin(ic, "configuration", attributes);
    assertSame(context, ic.peekObject());
    assertTrue(context.isPackagingDataEnabled());

    action.end(ic, "configuration");
    assertTrue(ic.isEmpty());
    assertEquals(1, statusCount("End of configuration."));
  }

  @Test
  public void scanSetToFalseDoesNotScheduleScanning() throws Exception {
    ConfigurationWatchListUtil.setMainWatchURL(context, URI.create("file:/logback.xml").toURL());
    addAttribute(ConfigurationAction.SCAN_ATTR, "FALSE");

    action.processScanAttrib(ic, attributes);

    assertNull(context.getObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK));
    assertTrue(context.getScheduledFutures().isEmpty());
    assertEquals(0, statusCount("Will scan for changes in .*"));
  }

  @Test
  public void scanWithoutMainConfigurationFileIsNotPossible() {
    addAttribute(ConfigurationAction.SCAN_ATTR, "true");

    action.processScanAttrib(ic, attributes);

    assertNull(context.getObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK));
    assertTrue(context.getScheduledFutures().isEmpty());
    assertEquals(Status.WARN, onlyStatus("Due to missing top level configuration file, reconfiguration on change "
        + "\\(configuration file scanning\\) cannot be done\\.").getLevel());
  }

  @Test
  public void scanWithoutScanPeriodUsesTheDefault() throws Exception {
    URL mainURL = URI.create("file:/logback.xml").toURL();
    ConfigurationWatchListUtil.setMainWatchURL(context, mainURL);
    addAttribute(ConfigurationAction.SCAN_ATTR, "true");

    action.processScanAttrib(ic, attributes);

    assertEquals(0, statusCount("Failed to parse .*"));
    assertScanningScheduledWithDefaultPeriod(mainURL);
  }

  @Test
  public void scanPeriodSetsTheScanningPeriod() throws Exception {
    URL mainURL = URI.create("file:/logback.xml").toURL();
    ConfigurationWatchListUtil.setMainWatchURL(context, mainURL);
    addAttribute(ConfigurationAction.SCAN_ATTR, "true");
    addAttribute(ConfigurationAction.SCAN_PERIOD_ATTR, "30 seconds");

    action.processScanAttrib(ic, attributes);

    assertTrue(context.getObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK) instanceof ReconfigureOnChangeTask);
    assertEquals(1, context.getScheduledFutures().size());
    assertEquals(1, statusCount("Will scan for changes in \\[" + mainURL + "\\] "));
    assertEquals(1, statusCount("Setting ReconfigureOnChangeTask scanning period to 30 seconds"));
    assertEquals(0, statusCount("Failed to parse .*"));
    assertEquals(0, statusCount("No 'scanPeriod' specified.*"));
  }

  @Test
  public void malformedScanPeriodFallsBackToTheDefault() throws Exception {
    URL mainURL = URI.create("file:/logback.xml").toURL();
    ConfigurationWatchListUtil.setMainWatchURL(context, mainURL);
    addAttribute(ConfigurationAction.SCAN_ATTR, "true");
    addAttribute(ConfigurationAction.SCAN_PERIOD_ATTR, "every now and then");

    action.processScanAttrib(ic, attributes);

    Status warning = onlyStatus("Failed to parse 'scanPeriod' attribute \\[every now and then\\]");
    assertEquals(Status.WARN, warning.getLevel());
    assertTrue(warning.getThrowable() instanceof IllegalArgumentException);
    assertScanningScheduledWithDefaultPeriod(mainURL);
  }

  @Test
  public void scanPeriodWithUnexpectedUnitFallsBackToTheDefault() throws Exception {
    URL mainURL = URI.create("file:/logback.xml").toURL();
    ConfigurationWatchListUtil.setMainWatchURL(context, mainURL);
    addAttribute(ConfigurationAction.SCAN_ATTR, "true");
    addAttribute(ConfigurationAction.SCAN_PERIOD_ATTR, "2 fortnights");
    IllegalStateException unexpectedUnit = new IllegalStateException("Unexpected fortnights");
    assertNotNull(ConfigurationAction.SCAN_PERIOD_DEFAULT); // initialized before Duration is mocked

    // Duration.valueOf() signals a unit it matched but cannot convert with an
    // IllegalStateException; its unit pattern currently only matches units it
    // converts, so the failure is injected
    try (MockedStatic<Duration> duration = mockStatic(Duration.class)) {
      duration.when(() -> Duration.valueOf("2 fortnights")).thenThrow(unexpectedUnit);

      action.processScanAttrib(ic, attributes);
    }

    Status warning = onlyStatus("Failed to parse 'scanPeriod' attribute \\[2 fortnights\\]");
    assertEquals(Status.WARN, warning.getLevel());
    assertSame(unexpectedUnit, warning.getThrowable());
    assertScanningScheduledWithDefaultPeriod(mainURL);
  }

  @Test
  public void getSystemPropertyReadsTheSystemProperty() {
    String key = "ConfigurationActionTest." + System.nanoTime();
    System.setProperty(key, "value");
    try {
      assertEquals("value", action.getSystemProperty(key));
    } finally {
      System.clearProperty(key);
    }
    assertNull(action.getSystemProperty(key));
  }

  @Test
  public void getSystemPropertyReturnsNullWhenAccessIsDenied() {
    final String key = "ConfigurationActionTest.denied";
    Properties original = System.getProperties();
    // System.getProperty() delegates to the installed Properties object, which
    // can refuse access the way a SecurityManager would
    Properties denying = new Properties(original) {
      private static final long serialVersionUID = 1L;

      @Override
      public String getProperty(String name) {
        if (key.equals(name)) {
          throw new SecurityException("access denied to " + name);
        }
        return super.getProperty(name);
      }
    };
    System.setProperties(denying);
    try {
      assertNull(action.getSystemProperty(key));
    } finally {
      System.setProperties(original);
    }
  }

  private void assertScanningScheduledWithDefaultPeriod(URL mainURL) {
    assertTrue(context.getObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK) instanceof ReconfigureOnChangeTask);
    assertEquals(1, context.getScheduledFutures().size());
    assertEquals(Status.INFO,
        onlyStatus("No 'scanPeriod' specified. Defaulting to " + ConfigurationAction.SCAN_PERIOD_DEFAULT).getLevel());
    assertEquals(1, statusCount("Will scan for changes in \\[" + mainURL + "\\] "));
    assertEquals(1, statusCount(
        "Setting ReconfigureOnChangeTask scanning period to " + ConfigurationAction.SCAN_PERIOD_DEFAULT));
  }

  private void addAttribute(String name, String value) {
    attributes.addAttribute("", name, name, "CDATA", value);
  }

  private int onConsoleStatusListeners() {
    int count = 0;
    for (StatusListener listener : context.getStatusManager().getCopyOfStatusListenerList()) {
      if (listener instanceof OnConsoleStatusListener) {
        count++;
      }
    }
    return count;
  }

  private List<Status> statusesMatching(String regex) {
    List<Status> found = new ArrayList<Status>();
    for (Status status : context.getStatusManager().getCopyOfStatusList()) {
      if (status.getMessage().matches(regex)) {
        found.add(status);
      }
    }
    return found;
  }

  private int statusCount(String regex) {
    return statusesMatching(regex).size();
  }

  private Status onlyStatus(String regex) {
    List<Status> found = statusesMatching(regex);
    assertEquals(found.toString(), 1, found.size());
    return found.get(0);
  }
}
