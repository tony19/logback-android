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
package ch.qos.logback.core.joran.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

public class BodyEventTest {

  private static LocatorImpl locatorAt(int line, int column) {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(line);
    locator.setColumnNumber(column);
    return locator;
  }

  @Test
  public void textIsTrimmed() {
    BodyEvent be = new BodyEvent("  some text \n", locatorAt(1, 1));
    assertEquals("some text", be.getText());
  }

  @Test
  public void nullTextIsReturnedAsNull() {
    BodyEvent be = new BodyEvent(null, locatorAt(1, 1));
    assertNull(be.getText());
  }

  @Test
  public void appendConcatenatesUntrimmedChunks() {
    BodyEvent be = new BodyEvent(" hello ", locatorAt(1, 1));
    be.append("world ");
    assertEquals("hello world", be.getText());
  }

  @Test
  public void toStringShowsTrimmedTextAndPosition() {
    BodyEvent be = new BodyEvent(" hi ", locatorAt(4, 12));
    assertEquals("BodyEvent(hi)4,12", be.toString());
  }
}
