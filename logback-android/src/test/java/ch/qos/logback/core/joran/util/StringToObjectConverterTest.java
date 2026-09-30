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
package ch.qos.logback.core.joran.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;

public class StringToObjectConverterTest {

  /** Has a valueOf(String) method, but not a static one. */
  public static class InstanceValueOf {
    public InstanceValueOf valueOf(String s) {
      return this;
    }
  }

  Context context = new ContextBase();
  ContextAwareBase ca = new ContextAwareBase();

  @Before
  public void setUp() {
    ca.setContext(context);
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  @Test
  public void canBeInstantiated() {
    assertFalse(new StringToObjectConverter().isBuildableFromSimpleString());
  }

  @Test
  public void primitivesAndJavaLangTypesCanBeBuiltFromSimpleString() {
    assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(int.class));
    assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(Integer.class));
    assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(String.class));
  }

  @Test
  public void typesWithStaticValueOfMethodCanBeBuiltFromSimpleString() {
    assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(Duration.class));
    assertFalse(StringToObjectConverter.canBeBuiltFromSimpleString(InstanceValueOf.class));
  }

  @Test
  public void charsetCanBeBuiltFromSimpleString() {
    assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(Charset.class));
  }

  @Test
  public void otherTypesCannotBeBuiltFromSimpleString() {
    assertFalse(StringToObjectConverter.canBeBuiltFromSimpleString(Door.class));
    // array classes have no package
    assertFalse(StringToObjectConverter.canBeBuiltFromSimpleString(String[].class));
  }

  @Test
  public void enumCanBeBuiltFromSimpleStringEvenWithoutValueOfMethod() {
    // every enum has a static valueOf(String) method, so the enum rule only
    // applies when that method cannot be found
    try (MockedStatic<StringToObjectConverter> converter = mockStatic(StringToObjectConverter.class,
        CALLS_REAL_METHODS)) {
      converter.when(() -> StringToObjectConverter.getValueOfMethod(HouseColor.class)).thenReturn(null);
      assertNull(StringToObjectConverter.getValueOfMethod(HouseColor.class));

      assertTrue(StringToObjectConverter.canBeBuiltFromSimpleString(HouseColor.class));
    }
  }

  @Test
  public void getValueOfMethodFindsPublicValueOfTakingString() throws Exception {
    Method m = StringToObjectConverter.getValueOfMethod(Integer.class);
    assertNotNull(m);
    assertEquals(Integer.class.getMethod("valueOf", String.class), m);
    assertTrue(Modifier.isStatic(m.getModifiers()));
    assertNull(StringToObjectConverter.getValueOfMethod(Door.class));
  }

  @Test
  public void nullValueConvertsToNull() {
    assertNull(StringToObjectConverter.convertArg(ca, null, String.class));
  }

  @Test
  public void stringValueIsTrimmed() {
    assertEquals("abc", StringToObjectConverter.convertArg(ca, "  abc  ", String.class));
  }

  @Test
  public void primitiveNumbersAreParsed() {
    assertEquals(Integer.valueOf(42), StringToObjectConverter.convertArg(ca, " 42 ", int.class));
    assertEquals(Long.valueOf(42L), StringToObjectConverter.convertArg(ca, "42", long.class));
    assertEquals(Float.valueOf(1.5f), StringToObjectConverter.convertArg(ca, "1.5", float.class));
    assertEquals(Double.valueOf(2.5d), StringToObjectConverter.convertArg(ca, "2.5", double.class));
  }

  @Test
  public void booleanIsParsedIgnoringCase() {
    assertEquals(Boolean.TRUE, StringToObjectConverter.convertArg(ca, "TRUE", boolean.class));
    assertEquals(Boolean.FALSE, StringToObjectConverter.convertArg(ca, "False", boolean.class));
    assertNull(StringToObjectConverter.convertArg(ca, "maybe", boolean.class));
  }

  @Test
  public void enumIsConvertedByConstantName() {
    assertEquals(HouseColor.BLUE, StringToObjectConverter.convertArg(ca, "BLUE", HouseColor.class));
  }

  @Test
  public void staticValueOfMethodIsUsed() {
    Duration d = (Duration) StringToObjectConverter.convertArg(ca, "2 seconds", Duration.class);
    assertEquals(2000L, d.getMilliseconds());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void failingValueOfMethodIsReported() {
    assertNull(StringToObjectConverter.convertArg(ca, "abc", Integer.class));
    List<Status> list = statuses();
    assertEquals(1, list.size());
    assertEquals(Status.ERROR, list.get(0).getLevel());
    assertEquals("Failed to invoke valueOf{} method in class [java.lang.Integer] with value [abc]",
        list.get(0).getMessage());
  }

  @Test
  public void charsetIsLookedUpByName() {
    assertEquals(Charset.forName("UTF-8"), StringToObjectConverter.convertArg(ca, "UTF-8", Charset.class));
  }

  @Test
  public void unsupportedCharsetIsReported() {
    assertNull(StringToObjectConverter.convertArg(ca, "no-such-charset", Charset.class));
    List<Status> list = statuses();
    assertEquals(1, list.size());
    assertEquals(Status.ERROR, list.get(0).getLevel());
    assertEquals("Failed to get charset [no-such-charset]", list.get(0).getMessage());
  }

  @Test
  public void unsupportedTypeConvertsToNull() {
    assertNull(StringToObjectConverter.convertArg(ca, "x", Object.class));
    assertTrue(statuses().isEmpty());
  }
}
