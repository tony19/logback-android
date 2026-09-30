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

import java.lang.reflect.Field;

import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.joran.spi.ElementPath;

public class StartEventTest {

  private static LocatorImpl locatorAt(int line, int column) {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(line);
    locator.setColumnNumber(column);
    return locator;
  }

  private static StartEvent startEvent(AttributesImpl attributes) {
    return new StartEvent(new ElementPath("a/q"), "", "q", "q", attributes, locatorAt(2, 5));
  }

  @Test
  public void toStringListsAttributesAndPosition() {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "a", "a", "CDATA", "1");
    attributes.addAttribute("", "b", "b", "CDATA", "2");

    // (no separator between the name and the first attribute, as upstream)
    assertEquals("StartEvent(qa=\"1\" b=\"2\")  [2,5]", startEvent(attributes).toString());
  }

  @Test
  public void attributesAreASnapshot() {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "a", "a", "CDATA", "1");
    StartEvent event = startEvent(attributes);

    // the parser reuses its attributes object for the next element
    attributes.clear();
    attributes.addAttribute("", "z", "z", "CDATA", "9");

    assertEquals(1, event.getAttributes().getLength());
    assertEquals("1", event.getAttributes().getValue("a"));
    assertEquals("[a][q]", event.elementPath.toString());
  }

  @Test
  public void toStringToleratesMissingAttributes() throws Exception {
    StartEvent event = startEvent(new AttributesImpl());
    // the constructor always copies the attributes; null them out to exercise
    // toString's defensive check
    Field attributesField = StartEvent.class.getField("attributes");
    attributesField.setAccessible(true);
    attributesField.set(event, null);

    assertEquals("StartEvent(q)  [2,5]", event.toString());
  }
}
