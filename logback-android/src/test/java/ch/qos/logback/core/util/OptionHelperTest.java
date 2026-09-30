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
package ch.qos.logback.core.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.android.SystemPropertiesProxy;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;


public class OptionHelperTest  {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  String text = "Testing ${v1} variable substitution ${v2}";
  String expected = "Testing if variable substitution works";
  Context context = new ContextBase();
  Map<String, String> secondaryMap;
  
  
  
  @Before
  public void setUp() throws Exception {
    secondaryMap = new HashMap<String, String>();
  }

  @Test
  public void testLiteral() {
    String noSubst = "hello world";
    String result = OptionHelper.substVars(noSubst, context);
    assertEquals(noSubst, result);
  }

  @Test
  public void testUndefinedValues() {
    String withUndefinedValues = "${axyz}";
    
    String result = OptionHelper.substVars(withUndefinedValues, context);
    assertEquals("axyz"+OptionHelper._IS_UNDEFINED, result);
  }
  
  @Test
  public void testSubstVarsVariableNotClosed() {
    String noSubst = "testing if ${v1 works";
    
    try {
      @SuppressWarnings("unused")
      String result = OptionHelper.substVars(noSubst, context);
      fail();
    } catch (IllegalArgumentException e) {
      //ok
    }
  }
  @Test
  public void testSubstVarsContextOnly() {
    context.putProperty("v1", "if");
    context.putProperty("v2", "works");
    
    String result = OptionHelper.substVars(text, context);
    assertEquals(expected, result); 
  }
  
  @Test
  public void testSubstVarsSystemProperties() { 
    System.setProperty("v1", "if");
    System.setProperty("v2", "works");
    
    String result = OptionHelper.substVars(text, context);
    assertEquals(expected, result); 
    
    System.clearProperty("v1");
    System.clearProperty("v2");
  }
  
  @Test
  public void testSubstVarsWithDefault() {   
    context.putProperty("v1", "if");
    String textWithDefault = "Testing ${v1} variable substitution ${v2:-toto}";
    String resultWithDefault = "Testing if variable substitution toto";
    
    String result = OptionHelper.substVars(textWithDefault, context);
    assertEquals(resultWithDefault, result); 
  }
  
  @Test
  public void testSubstVarsRecursive() {
    context.putProperty("v1", "if");
    context.putProperty("v2", "${v3}");
    context.putProperty("v3", "works");
    
    String result = OptionHelper.substVars(text, context);
    assertEquals(expected, result); 
  }

  @Test
  public void testSubstVarsTwoLevelsDeep() {
    context.putProperty("v1", "if");
    context.putProperty("v2", "${v3}");
    context.putProperty("v3", "${v4}");
    context.putProperty("v4", "works");

    String result = OptionHelper.substVars(text, context);
    assertEquals(expected, result);
  }

  @Test
  public void testSubstVarsTwoLevelsWithDefault() {
    // Example input taken from LOGBCK-943 bug report
    context.putProperty("APP_NAME", "LOGBACK");
    context.putProperty("ARCHIVE_SUFFIX", "archive.log");
    context.putProperty("LOG_HOME", "${logfilepath.default:-logs}");
    context.putProperty("ARCHIVE_PATH", "${LOG_HOME}/archive/${APP_NAME}");

    String result = OptionHelper.substVars("${ARCHIVE_PATH}_trace_${ARCHIVE_SUFFIX}", context);
    assertEquals("logs/archive/LOGBACK_trace_archive.log", result);
  }


  @Test(timeout = 1000)
  public void stubstVarsShouldNotGoIntoInfiniteLoop() {
    context.putProperty("v1", "if");
    context.putProperty("v2", "${v3}");
    context.putProperty("v3", "${v4}");
    context.putProperty("v4", "${v2}c");

    expectedException.expect(Exception.class);
    OptionHelper.substVars(text, context);
  }

  @Test
  public void nonCircularGraphShouldWork() {
    context.putProperty("A", "${B} and ${C}");
    context.putProperty("B", "${B1}");
    context.putProperty("B1", "B1-value");
    context.putProperty("C", "${C1} and ${B}");
    context.putProperty("C1", "C1-value");

    String result = OptionHelper.substVars("${A}", context);
    assertEquals("B1-value and C1-value and B1-value", result);
  }

  @Test(timeout = 1000)
  public void detectCircularReferences0() {
    context.putProperty("A", "${A}");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${A} --> ${A}]");
    OptionHelper.substVars("${A}", context);
  }

  @Test(timeout = 1000)
  public void detectCircularReferences1() {
    context.putProperty("A", "${A}a");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${A} --> ${A}]");
    OptionHelper.substVars("${A}", context);
  }

  @Test(timeout = 1000)
  public void detectCircularReferences2() {
    context.putProperty("A", "${B}");
    context.putProperty("B", "${C}");
    context.putProperty("C", "${A}");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${A} --> ${B} --> ${C} --> ${A}]");
    OptionHelper.substVars("${A}", context);
  }


  @Test
  public void detectCircularReferencesInDefault() {
    context.putProperty("A", "${B:-${A}}");
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${A} --> ${B} --> ${A}]");
    OptionHelper.substVars("${A}", context);
  }

  @Test(timeout = 1000)
  public void detectCircularReferences3() {
    context.putProperty("A", "${B}");
    context.putProperty("B", "${C}");
    context.putProperty("C", "${A}");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${B} --> ${C} --> ${A} --> ${B}]");
    OptionHelper.substVars("${B} ", context);
  }

  @Test(timeout = 1000)
  public void detectCircularReferences4() {
    context.putProperty("A", "${B}");
    context.putProperty("B", "${C}");
    context.putProperty("C", "${A}");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${C} --> ${A} --> ${B} --> ${C}]");
    OptionHelper.substVars("${C} and ${A}", context);
  }

  @Test
  public void detectCircularReferences5() {
    context.putProperty("A", "${B} and ${C}");
    context.putProperty("B", "${B1}");
    context.putProperty("B1", "B1-value");
    context.putProperty("C", "${C1}");
    context.putProperty("C1", "here's the loop: ${A}");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Circular variable reference detected while parsing input [${A} --> ${C} --> ${C1} --> ${A}]");
    String result = OptionHelper.substVars("${A}", context);
    System.err.println(result);
  }

  @Test
  public void defaultValueReferencingAVariable() {
    context.putProperty("v1", "k1");
    String result = OptionHelper.substVars("${undef:-${v1}}", context);
    assertEquals("k1", result);
  }

  @Test
  public void jackrabbit_standalone() {
    String r = OptionHelper.substVars("${jackrabbit.log:-${repo:-jackrabbit}/log/jackrabbit.log}", context);
    assertEquals("jackrabbit/log/jackrabbit.log", r);
  }

  @Test
  public void doesNotThrowNullPointerExceptionForEmptyVariable() throws JoranException {
    context.putProperty("var", "");
    OptionHelper.substVars("${var}", context);
  }

  @Test
  public void trailingColon_LOGBACK_1140() {
    String prefix = "c:";
    String suffix = "/tmp";
    context.putProperty("var", prefix);
    String r = OptionHelper.substVars("${var}" + suffix, context);
    assertEquals(prefix + suffix, r);
  }

  @Test
  public void curlyBraces_LOGBACK_1101() {
    {
      String input = "foo{bar}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
    {
      String input = "{foo{\"bar\"}}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
    {
      String input = "a:{y}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
    {
      String input = "{world:{yay}}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
    {
      String input = "{hello:{world:yay}}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
    {
      String input = "{\"hello\":{\"world\":\"yay\"}}";
      String r = OptionHelper.substVars(input, context);
      assertEquals(input, r);
    }
  }

  @Test
  public void substVarsWrapsScanExceptions() {
    String input = "${a:-b:-c}";
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> OptionHelper.substVars(input, context));
    assertEquals("Failed to parse input [" + input + "]", e.getMessage());
    assertTrue(e.getCause() instanceof ScanException);
  }

  @Test
  public void isInstantiable() {
    // the class only has static members, but its implicit constructor is public
    assertNotNull(new OptionHelper());
  }

  @Test
  public void instantiateByClassNameUsesTheNoArgConstructor() throws Exception {
    Object o = OptionHelper.instantiateByClassName(ContextBase.class.getName(), Context.class, context);
    assertTrue(o instanceof ContextBase);
  }

  @Test
  public void instantiateByClassNameAndParameterUsesTheMatchingConstructor() throws Exception {
    Object o = OptionHelper.instantiateByClassNameAndParameter(FileSize.class.getName(), FileSize.class,
        getClass().getClassLoader(), long.class, 42L);
    assertEquals(42L, ((FileSize) o).getSize());
  }

  @Test
  public void instantiateByClassNameRejectsNullClassName() {
    assertThrows(NullPointerException.class,
        () -> OptionHelper.instantiateByClassName(null, Object.class, getClass().getClassLoader()));
  }

  @Test
  public void instantiateByClassNameRejectsIncompatibleClass() {
    IncompatibleClassException e = assertThrows(IncompatibleClassException.class,
        () -> OptionHelper.instantiateByClassName(FileSize.class.getName(), Context.class, context));
    assertSame(Context.class, e.requestedClass);
    assertSame(FileSize.class, e.obtainedClass);
  }

  @Test
  public void instantiateByClassNameWrapsLoadingFailures() {
    DynamicClassLoadingException e = assertThrows(DynamicClassLoadingException.class,
        () -> OptionHelper.instantiateByClassName("no.such.Clazz", Object.class, context));
    assertEquals("Failed to instantiate type no.such.Clazz", e.getMessage());
    assertTrue(e.getCause() instanceof ClassNotFoundException);
  }

  static final String DENIED_KEY = "optionHelperTest.denied";

  @Test
  public void getSystemPropertyWithDefaultReturnsDefaultWhenAccessIsDenied() {
    withDeniedSystemProperty(() -> assertEquals("def", OptionHelper.getSystemProperty(DENIED_KEY, "def")));
  }

  @Test
  public void getSystemPropertyWithDefaultReturnsValueOrDefault() {
    System.setProperty(DENIED_KEY, "value");
    try {
      assertEquals("value", OptionHelper.getSystemProperty(DENIED_KEY, "def"));
    } finally {
      System.clearProperty(DENIED_KEY);
    }
    assertEquals("def", OptionHelper.getSystemProperty(DENIED_KEY, "def"));
  }

  @Test
  public void getSystemPropertyReturnsNullWhenAccessIsDenied() {
    withDeniedSystemProperty(() -> assertNull(OptionHelper.getSystemProperty(DENIED_KEY)));
  }

  @Test
  public void getSystemPropertyPrefersSystemPropertyOverAndroidProperty() {
    SystemPropertiesProxy proxy = mock(SystemPropertiesProxy.class);
    when(proxy.get(DENIED_KEY, null)).thenReturn("android");
    try (MockedStatic<SystemPropertiesProxy> mocked = mockStatic(SystemPropertiesProxy.class)) {
      mocked.when(SystemPropertiesProxy::getInstance).thenReturn(proxy);

      assertEquals("android", OptionHelper.getSystemProperty(DENIED_KEY));
      System.setProperty(DENIED_KEY, "system");
      try {
        assertEquals("system", OptionHelper.getSystemProperty(DENIED_KEY));
      } finally {
        System.clearProperty(DENIED_KEY);
      }
    }
  }

  @Test
  public void getEnvReadsTheEnvironment() {
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      assertEquals(entry.getValue(), OptionHelper.getEnv(entry.getKey()));
    }
    assertNull(OptionHelper.getEnv("OPTION_HELPER_TEST_UNDEFINED_VARIABLE"));
  }

  @Test
  public void getSystemPropertiesReturnsTheSystemProperties() {
    assertSame(System.getProperties(), OptionHelper.getSystemProperties());
  }

  @Test
  public void getAndroidSystemPropertyReturnsNullForRejectedKey() {
    SystemPropertiesProxy proxy = mock(SystemPropertiesProxy.class);
    when(proxy.get(DENIED_KEY, null)).thenThrow(new IllegalArgumentException("key too long"));
    try (MockedStatic<SystemPropertiesProxy> mocked = mockStatic(SystemPropertiesProxy.class)) {
      mocked.when(SystemPropertiesProxy::getInstance).thenReturn(proxy);

      assertNull(OptionHelper.getAndroidSystemProperty(DENIED_KEY));
    }
  }

  @Test
  public void setSystemPropertiesSetsEveryProperty() {
    Properties props = new Properties();
    props.setProperty("optionHelperTest.a", "1");
    props.setProperty("optionHelperTest.b", "2");
    ContextAwareBase contextAware = new ContextAwareBase();
    contextAware.setContext(context);
    try {
      OptionHelper.setSystemProperties(contextAware, props);

      assertEquals("1", System.getProperty("optionHelperTest.a"));
      assertEquals("2", System.getProperty("optionHelperTest.b"));
      assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
    } finally {
      System.clearProperty("optionHelperTest.a");
      System.clearProperty("optionHelperTest.b");
    }
  }

  @Test
  public void setSystemPropertyReportsDeniedAccess() {
    final ContextAwareBase contextAware = new ContextAwareBase();
    contextAware.setContext(context);

    withDeniedSystemProperty(() -> OptionHelper.setSystemProperty(contextAware, DENIED_KEY, "value"));

    assertNull(System.getProperty(DENIED_KEY));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Failed to set system property [" + DENIED_KEY + "]", statuses.get(0).getMessage());
    assertTrue(statuses.get(0).getThrowable() instanceof SecurityException);
  }

  @Test
  public void extractDefaultReplacementSplitsKeyAndDefaultValue() {
    assertArrayEquals(new String[] { "key", "def" }, OptionHelper.extractDefaultReplacement("key:-def"));
    assertArrayEquals(new String[] { "key", "" }, OptionHelper.extractDefaultReplacement("key:-"));
    assertArrayEquals(new String[] { "key", null }, OptionHelper.extractDefaultReplacement("key"));
    assertArrayEquals(new String[] { null, null }, OptionHelper.extractDefaultReplacement(null));
  }

  @Test
  public void toBooleanParsesTrueAndFalseIgnoringCaseAndWhitespace() {
    assertTrue(OptionHelper.toBoolean("true", false));
    assertTrue(OptionHelper.toBoolean(" TRUE ", false));
    assertFalse(OptionHelper.toBoolean("false", true));
    assertFalse(OptionHelper.toBoolean(" False ", true));
  }

  @Test
  public void toBooleanFallsBackToDefault() {
    assertTrue(OptionHelper.toBoolean(null, true));
    assertFalse(OptionHelper.toBoolean(null, false));
    assertTrue(OptionHelper.toBoolean("yes", true));
    assertFalse(OptionHelper.toBoolean("yes", false));
  }

  @Test
  public void isEmptyIsTrueOnlyForNullOrEmptyString() {
    assertTrue(OptionHelper.isEmpty(null));
    assertTrue(OptionHelper.isEmpty(""));
    assertFalse(OptionHelper.isEmpty(" "));
    assertFalse(OptionHelper.isEmpty("a"));
  }

  /**
   * Runs {@code r} with system properties that throw a
   * {@link SecurityException} on any access to {@link #DENIED_KEY}, like a
   * security manager denying that access would.
   */
  private static void withDeniedSystemProperty(Runnable r) {
    Properties original = System.getProperties();
    System.setProperties(new DenyingProperties(original, DENIED_KEY));
    try {
      r.run();
    } finally {
      System.setProperties(original);
    }
  }

  /** A copy of other properties that denies access to one key. */
  private static class DenyingProperties extends Properties {
    private static final long serialVersionUID = 1L;
    private final String deniedKey;

    DenyingProperties(Properties source, String deniedKey) {
      this.deniedKey = deniedKey;
      putAll(source);
    }

    @Override
    public String getProperty(String key) {
      check(key);
      return super.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
      check(key);
      return super.getProperty(key, defaultValue);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
      check(key);
      return super.setProperty(key, value);
    }

    private void check(String key) {
      if (deniedKey.equals(key)) {
        throw new SecurityException("access to [" + key + "] denied");
      }
    }
  }
}
