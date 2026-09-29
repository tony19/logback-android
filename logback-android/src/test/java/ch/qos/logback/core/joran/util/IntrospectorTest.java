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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

public class IntrospectorTest {

  public static class Bean {
    public String get() {
      return null;
    }

    public void set(String value) {
    }

    public String getName() {
      return null;
    }

    public void setName(String name) {
    }

    public Object getValue() {
      return null;
    }

    public void setValue(String value) {
    }

    public int getCount() {
      return 0;
    }

    public void setPair(String first, String second) {
    }

    public String getItem(int index) {
      return null;
    }
  }

  public static class ValueGetter {
    public Object getValue() {
      return null;
    }
  }

  /** Declares the setter and inherits the getter. */
  public static class InheritedGetterBean extends ValueGetter {
    public void setValue(String value) {
    }
  }

  public static class ValueSetter {
    public void setValue(String value) {
    }
  }

  /** Declares the getter and inherits the setter. */
  public static class InheritedSetterBean extends ValueSetter {
    public Object getValue() {
      return null;
    }
  }

  private static Map<String, PropertyDescriptor> descriptorsOf(Class<?> clazz) {
    Map<String, PropertyDescriptor> map = new HashMap<String, PropertyDescriptor>();
    for (PropertyDescriptor pd : Introspector.getPropertyDescriptors(clazz)) {
      map.put(pd.getName(), pd);
    }
    return map;
  }

  @Test
  public void canBeInstantiated() {
    assertNotNull(new Introspector());
  }

  @Test
  public void decapitalizeLowersOnlyTheFirstLetter() {
    assertNull(Introspector.decapitalize(null));
    assertEquals("", Introspector.decapitalize(""));
    assertEquals("x", Introspector.decapitalize("X"));
    assertEquals("aB", Introspector.decapitalize("AB"));
    assertEquals("fooBar", Introspector.decapitalize("FooBar"));
  }

  @Test
  public void bareGetAndSetMethodsAreNotProperties() {
    Map<String, PropertyDescriptor> map = descriptorsOf(Bean.class);
    // "class" comes from Object.getClass()
    assertEquals(new HashSet<String>(Arrays.asList("class", "name", "value", "count", "pair", "item")),
        map.keySet());
  }

  @Test
  public void getterAndSetterMakeAReadWriteProperty() throws Exception {
    PropertyDescriptor pd = descriptorsOf(Bean.class).get("name");
    assertEquals(Bean.class.getMethod("getName"), pd.getReadMethod());
    assertEquals(Bean.class.getMethod("setName", String.class), pd.getWriteMethod());
    assertEquals(String.class, pd.getPropertyType());
  }

  @Test
  public void setterTypeTakesPriorityOverGetterType() throws Exception {
    PropertyDescriptor pd = descriptorsOf(Bean.class).get("value");
    assertEquals(Bean.class.getMethod("getValue"), pd.getReadMethod());
    assertEquals(String.class, pd.getPropertyType());
  }

  @Test
  public void setterTypeTakesPriorityWhicheverAccessorIsVisitedFirst() throws Exception {
    // Class.getMethods() usually lists a class's own methods before the
    // inherited ones, so these two beans present the setter and the getter of
    // "value" in opposite orders. The setter's type must win either way.
    PropertyDescriptor setterFirst = descriptorsOf(InheritedGetterBean.class).get("value");
    assertEquals(ValueGetter.class.getMethod("getValue"), setterFirst.getReadMethod());
    assertEquals(InheritedGetterBean.class.getMethod("setValue", String.class), setterFirst.getWriteMethod());
    assertEquals(String.class, setterFirst.getPropertyType());

    PropertyDescriptor getterFirst = descriptorsOf(InheritedSetterBean.class).get("value");
    assertEquals(InheritedSetterBean.class.getMethod("getValue"), getterFirst.getReadMethod());
    assertEquals(ValueSetter.class.getMethod("setValue", String.class), getterFirst.getWriteMethod());
    assertEquals(String.class, getterFirst.getPropertyType());
  }

  @Test
  public void getterAloneMakesAReadOnlyProperty() throws Exception {
    PropertyDescriptor pd = descriptorsOf(Bean.class).get("count");
    assertEquals(Bean.class.getMethod("getCount"), pd.getReadMethod());
    assertNull(pd.getWriteMethod());
    assertEquals(int.class, pd.getPropertyType());
  }

  @Test
  public void setterWithTwoParametersIsIgnored() {
    PropertyDescriptor pd = descriptorsOf(Bean.class).get("pair");
    assertNull(pd.getWriteMethod());
    assertNull(pd.getReadMethod());
    assertNull(pd.getPropertyType());
  }

  @Test
  public void getterWithParameterIsIgnored() {
    PropertyDescriptor pd = descriptorsOf(Bean.class).get("item");
    assertNull(pd.getReadMethod());
    assertNull(pd.getWriteMethod());
    assertNull(pd.getPropertyType());
  }

  @Test
  public void methodDescriptorsListThePublicMethods() throws Exception {
    MethodDescriptor[] descriptors = Introspector.getMethodDescriptors(Bean.class);
    assertEquals(Bean.class.getMethods().length, descriptors.length);
    boolean found = false;
    for (MethodDescriptor md : descriptors) {
      assertEquals(md.getName(), md.getMethod().getName());
      if (md.getMethod().equals(Bean.class.getMethod("setPair", String.class, String.class))) {
        found = true;
      }
    }
    assertTrue(found);
  }
}
