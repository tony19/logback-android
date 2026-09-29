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
package ch.qos.logback.classic.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

public class LoggerContextVOTest {

  static final long BIRTH_TIME = 0x123456789AL;

  static Map<String, String> props(String key, String value) {
    Map<String, String> map = new HashMap<String, String>();
    map.put(key, value);
    return map;
  }

  static String distinctCopy(String s) {
    return new StringBuilder(s).toString();
  }

  @Test
  public void threeArgConstructorExposesGivenValues() {
    Map<String, String> map = props("k", "v");
    LoggerContextVO vo = new LoggerContextVO("ctx", map, BIRTH_TIME);

    assertEquals("ctx", vo.getName());
    assertSame(map, vo.getPropertyMap());
    assertEquals(BIRTH_TIME, vo.getBirthTime());
  }

  @Test
  public void loggerContextConstructorCopiesNamePropertiesAndBirthTime() {
    LoggerContext lc = new LoggerContext();
    lc.setName("fromContext");
    lc.putProperty("key", "value");

    LoggerContextVO vo = new LoggerContextVO(lc);

    assertEquals("fromContext", vo.getName());
    assertEquals("value", vo.getPropertyMap().get("key"));
    assertEquals(lc.getBirthTime(), vo.getBirthTime());

    // the property map is a snapshot, later changes to the context are not seen
    lc.putProperty("late", "x");
    assertFalse(vo.getPropertyMap().containsKey("late"));
  }

  @Test
  public void toStringListsNamePropertiesAndBirthTime() {
    LoggerContextVO vo = new LoggerContextVO("ctx", props("k", "v"), 42L);
    assertEquals("LoggerContextVO{name='ctx', propertyMap={k=v}, birthTime=42}", vo.toString());
  }

  @Test
  public void toStringRendersNullFieldsAsNull() {
    LoggerContextVO vo = new LoggerContextVO(null, null, 0L);
    assertEquals("LoggerContextVO{name='null', propertyMap=null, birthTime=0}", vo.toString());
  }

  @Test
  public void equalsIsReflexive() {
    LoggerContextVO vo = new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME);
    assertTrue(vo.equals(vo));
  }

  @Test
  public void equalsRejectsNullAndOtherTypes() {
    LoggerContextVO vo = new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME);
    assertFalse(vo.equals(null));
    assertFalse(vo.equals("ctx"));
  }

  @Test
  public void equalsHoldsForSameValuesInDistinctInstances() {
    LoggerContextVO a = new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME);
    LoggerContextVO b = new LoggerContextVO(distinctCopy("ctx"), props("k", "v"), BIRTH_TIME);
    assertNotSame(a.getName(), b.getName());
    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void equalsHoldsWhenNameAndPropertiesAreNullOnBothSides() {
    LoggerContextVO a = new LoggerContextVO(null, null, BIRTH_TIME);
    LoggerContextVO b = new LoggerContextVO(null, null, BIRTH_TIME);
    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void equalsFailsWhenBirthTimeDiffers() {
    LoggerContextVO a = new LoggerContextVO("ctx", props("k", "v"), 1L);
    LoggerContextVO b = new LoggerContextVO("ctx", props("k", "v"), 2L);
    assertFalse(a.equals(b));
  }

  @Test
  public void equalsFailsWhenNamesDiffer() {
    LoggerContextVO a = new LoggerContextVO("ctx", null, BIRTH_TIME);
    LoggerContextVO b = new LoggerContextVO("other", null, BIRTH_TIME);
    assertFalse(a.equals(b));
  }

  @Test
  public void equalsFailsWhenOnlyOneNameIsNull() {
    LoggerContextVO named = new LoggerContextVO("ctx", null, BIRTH_TIME);
    LoggerContextVO unnamed = new LoggerContextVO(null, null, BIRTH_TIME);
    assertFalse(named.equals(unnamed));
    assertFalse(unnamed.equals(named));
  }

  @Test
  public void equalsFailsWhenPropertyMapsDiffer() {
    LoggerContextVO a = new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME);
    LoggerContextVO b = new LoggerContextVO("ctx", props("k", "other"), BIRTH_TIME);
    assertFalse(a.equals(b));
  }

  @Test
  public void equalsFailsWhenOnlyOnePropertyMapIsNull() {
    LoggerContextVO withMap = new LoggerContextVO("ctx", Collections.<String, String>emptyMap(), BIRTH_TIME);
    LoggerContextVO withoutMap = new LoggerContextVO("ctx", null, BIRTH_TIME);
    assertFalse(withMap.equals(withoutMap));
    assertFalse(withoutMap.equals(withMap));
  }

  @Test
  public void hashCodeOfNullNameAndPropertiesDependsOnlyOnBirthTime() {
    LoggerContextVO vo = new LoggerContextVO(null, null, BIRTH_TIME);
    assertEquals((int) (BIRTH_TIME ^ (BIRTH_TIME >>> 32)), vo.hashCode());
  }

  @Test
  public void hashCodeCombinesNamePropertiesAndBirthTime() {
    Map<String, String> map = props("k", "v");
    LoggerContextVO vo = new LoggerContextVO("ctx", map, BIRTH_TIME);
    int expected = 31 * (31 * "ctx".hashCode() + map.hashCode()) + (int) (BIRTH_TIME ^ (BIRTH_TIME >>> 32));
    assertEquals(expected, vo.hashCode());
  }

  @Test
  public void hashCodeChangesWithEachField() {
    LoggerContextVO base = new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME);
    assertNotEquals(base.hashCode(), new LoggerContextVO("other", props("k", "v"), BIRTH_TIME).hashCode());
    assertNotEquals(base.hashCode(), new LoggerContextVO("ctx", props("k", "w"), BIRTH_TIME).hashCode());
    assertNotEquals(base.hashCode(), new LoggerContextVO("ctx", props("k", "v"), BIRTH_TIME + 1).hashCode());
  }
}
