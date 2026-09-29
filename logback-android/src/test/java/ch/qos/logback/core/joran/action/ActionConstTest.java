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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests {@link ActionConst}.
 */
public class ActionConstTest {

  @Test
  public void oneStringParamDescribesASingleStringParameter() {
    assertArrayEquals(new Class<?>[] { String.class }, ActionConst.ONE_STRING_PARAM);
  }

  @Test
  public void attributeAndTagNamesMatchTheConfigurationSyntax() {
    assertEquals("appender", ActionConst.APPENDER_TAG);
    assertEquals("ref", ActionConst.REF_ATTRIBUTE);
    assertEquals("additivity", ActionConst.ADDITIVITY_ATTRIBUTE);
    assertEquals("level", ActionConst.LEVEL_ATTRIBUTE);
    assertEquals("converterClass", ActionConst.CONVERTER_CLASS_ATTRIBUTE);
    assertEquals("conversionWord", ActionConst.CONVERSION_WORD_ATTRIBUTE);
    assertEquals("pattern", ActionConst.PATTERN_ATTRIBUTE);
    assertEquals("value", ActionConst.VALUE_ATTR);
    assertEquals("actionClass", ActionConst.ACTION_CLASS_ATTRIBUTE);
    assertEquals("INHERITED", ActionConst.INHERITED);
    assertEquals("NULL", ActionConst.NULL);
    assertEquals("APPENDER_BAG", ActionConst.APPENDER_BAG);
  }

  @Test
  public void canBeExtendedToShareTheConstants() {
    ActionConst constants = new ActionConst() {
    };
    assertEquals(ActionConst.class, constants.getClass().getSuperclass());
  }
}
