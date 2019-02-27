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

import java.util.HashMap;

import org.xml.sax.Attributes;

public class DummyAttributes implements Attributes {

  HashMap<String, String> atts = new HashMap<String, String>();

  public int getIndex(String qName) {
    return 0;
  }

  public int getIndex(String uri, String localName) {
    return 0;
  }

  public int getLength() {
    return 0;
  }

  public String getLocalName(int index) {
    return null;
  }

  public String getQName(int index) {
    return null;
  }

  public String getType(int index) {
    return null;
  }

  public String getType(String qName) {
    return null;
  }

  public String getType(String uri, String localName) {
    return null;
  }

  public String getURI(int index) {
    return null;
  }

  public String getValue(int index) {
    return null;
  }

  public String getValue(String qName) {
    return atts.get(qName);
  }

  public void setValue(String key, String value) {
    atts.put(key, value);
  }

  public String getValue(String uri, String localName) {
    return null;
  }

}
