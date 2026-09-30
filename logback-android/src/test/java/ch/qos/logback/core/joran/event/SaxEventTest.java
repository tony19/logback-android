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
import static org.junit.Assert.assertNotSame;

import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

public class SaxEventTest {

  @Test
  public void gettersReturnConstructorArguments() {
    SaxEvent event = new SaxEvent("urn:ns", "local", "p:local", new LocatorImpl());

    assertEquals("urn:ns", event.getNamespaceURI());
    assertEquals("local", event.getLocalName());
    assertEquals("p:local", event.getQName());
  }

  @Test
  public void locatorIsASnapshotOfTheParserPosition() {
    LocatorImpl parserLocator = new LocatorImpl();
    parserLocator.setSystemId("file:/config.xml");
    parserLocator.setLineNumber(7);
    parserLocator.setColumnNumber(3);

    SaxEvent event = new SaxEvent("", "x", "x", parserLocator);
    // the parser moves on
    parserLocator.setLineNumber(8);
    parserLocator.setColumnNumber(1);

    assertNotSame(parserLocator, event.getLocator());
    assertEquals("file:/config.xml", event.getLocator().getSystemId());
    assertEquals(7, event.getLocator().getLineNumber());
    assertEquals(3, event.getLocator().getColumnNumber());
  }
}
