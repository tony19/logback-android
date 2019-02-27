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


public class BodyEvent extends SaxEvent {

  private String text;

  BodyEvent(String text, Locator locator) {
    super(null, null, null, locator);
    this.text = text;
  }

  /**
   * Gets the body text
   *
   * @return the body text
   */
  public String getText() {
    if(text != null) {
      return text.trim();
    }
    return text;
  }

  @Override
  public String toString() {
    return "BodyEvent(" + getText() + ")" + locator.getLineNumber() + ","
        + locator.getColumnNumber();
  }

  public void append(String str) {
    text += str;
  }

}
