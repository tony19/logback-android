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

import ch.qos.logback.core.joran.spi.ElementPath;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

public class StartEvent extends SaxEvent {

  final public Attributes attributes;
  final public ElementPath elementPath;
  
  StartEvent(ElementPath elementPath, String namespaceURI, String localName, String qName,
      Attributes attributes, Locator locator) {
    super(namespaceURI, localName, qName, locator);
    // locator impl is used to take a snapshot!
    this.attributes = new AttributesImpl(attributes);
    this.elementPath = elementPath;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder("StartEvent(");
    b.append(getQName());
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        if (i > 0)
          b.append(' ');
        b.append(attributes.getLocalName(i)).append("=\"").append(attributes.getValue(i)).append("\"");
      }
    }
    b.append(")  [");
    b.append(locator.getLineNumber());
    b.append(",");
    b.append(locator.getColumnNumber());
    b.append("]");
    return b.toString();
  }

}
