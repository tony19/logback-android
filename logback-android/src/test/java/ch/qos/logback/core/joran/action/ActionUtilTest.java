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
package ch.qos.logback.core.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.joran.spi.InterpretationContext;

/**
 * Tests {@link ActionUtil}.
 */
public class ActionUtilTest {

  static final String KEY = "ActionUtilTest.key";
  static final String OTHER_KEY = "ActionUtilTest.otherKey";

  Context context = new ContextBase();
  InterpretationContext ic = new InterpretationContext(context, null);

  @Before
  public void setUp() {
    System.clearProperty(KEY);
    System.clearProperty(OTHER_KEY);
  }

  @After
  public void tearDown() {
    System.clearProperty(KEY);
    System.clearProperty(OTHER_KEY);
  }

  @Test
  public void stringToScopeIsCaseInsensitiveAndDefaultsToLocal() {
    assertEquals(Scope.SYSTEM, ActionUtil.stringToScope("system"));
    assertEquals(Scope.SYSTEM, ActionUtil.stringToScope("SYSTEM"));
    assertEquals(Scope.CONTEXT, ActionUtil.stringToScope("Context"));
    assertEquals(Scope.LOCAL, ActionUtil.stringToScope("local"));
    assertEquals(Scope.LOCAL, ActionUtil.stringToScope("bogus"));
    assertEquals(Scope.LOCAL, ActionUtil.stringToScope(null));
  }

  @Test
  public void setPropertyInLocalScopeOnlyAddsASubstitutionProperty() {
    ActionUtil.setProperty(ic, KEY, " v ", Scope.LOCAL);

    assertEquals("v", ic.getCopyOfPropertyMap().get(KEY));
    assertNull(context.getProperty(KEY));
    assertNull(System.getProperty(KEY));
  }

  @Test
  public void setPropertyInContextScopeOnlySetsAContextProperty() {
    ActionUtil.setProperty(ic, KEY, "v", Scope.CONTEXT);

    assertEquals("v", context.getProperty(KEY));
    assertFalse(ic.getCopyOfPropertyMap().containsKey(KEY));
    assertNull(System.getProperty(KEY));
  }

  @Test
  public void setPropertyInSystemScopeOnlySetsASystemProperty() {
    ActionUtil.setProperty(ic, KEY, "v", Scope.SYSTEM);

    assertEquals("v", System.getProperty(KEY));
    assertNull(context.getProperty(KEY));
    assertFalse(ic.getCopyOfPropertyMap().containsKey(KEY));
  }

  @Test
  public void setPropertiesInLocalScopeOnlyAddsSubstitutionProperties() {
    ActionUtil.setProperties(ic, twoProperties(), Scope.LOCAL);

    assertEquals("v1", ic.getCopyOfPropertyMap().get(KEY));
    assertEquals("v2", ic.getCopyOfPropertyMap().get(OTHER_KEY));
    assertNull(context.getProperty(KEY));
    assertNull(System.getProperty(KEY));
  }

  @Test
  public void setPropertiesInContextScopeOnlySetsContextProperties() {
    ActionUtil.setProperties(ic, twoProperties(), Scope.CONTEXT);

    assertEquals("v1", context.getProperty(KEY));
    assertEquals("v2", context.getProperty(OTHER_KEY));
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());
    assertNull(System.getProperty(KEY));
  }

  @Test
  public void setPropertiesInSystemScopeOnlySetsSystemProperties() {
    ActionUtil.setProperties(ic, twoProperties(), Scope.SYSTEM);

    assertEquals("v1", System.getProperty(KEY));
    assertEquals("v2", System.getProperty(OTHER_KEY));
    assertNull(context.getProperty(KEY));
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());
  }

  @Test
  public void nullScopeIsRejected() {
    assertThrows(NullPointerException.class, () -> ActionUtil.setProperty(ic, KEY, "v", null));
    assertThrows(NullPointerException.class, () -> ActionUtil.setProperties(ic, twoProperties(), null));

    assertNull(System.getProperty(KEY));
    assertNull(context.getProperty(KEY));
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());
  }

  @Test
  public void canBeInstantiated() {
    assertNotNull(new ActionUtil());
  }

  private Properties twoProperties() {
    Properties props = new Properties();
    props.setProperty(KEY, "v1");
    props.setProperty(OTHER_KEY, "v2");
    return props;
  }
}
