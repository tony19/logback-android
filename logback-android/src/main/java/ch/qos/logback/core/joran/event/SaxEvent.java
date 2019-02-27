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

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

public class SaxEvent {

  final public String namespaceURI;
  final public String localName;
  final public String qName;
  final public Locator locator;

  SaxEvent(String namespaceURI, String localName, String qName, Locator locator) {
    this.namespaceURI = namespaceURI;
    this.localName = localName;
    this.qName = qName;
    // locator impl is used to take a snapshot!
    this.locator = new LocatorImpl(locator);
  }

  public String getLocalName() {
    return localName;
  }

  public Locator getLocator() {
    return locator;
  }

  public String getNamespaceURI() {
    return namespaceURI;
  }

  public String getQName() {
    return qName;
  }
}
