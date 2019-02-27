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
package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;

public final class PropertyConverter extends ClassicConverter {

  String key;

  public void start() {
    String optStr = getFirstOption();
    if (optStr != null) {
      key = optStr;
      super.start();
    }
  }

  public String convert(ILoggingEvent event) {
    if (key == null) {
      return "Property_HAS_NO_KEY";
    } else {
      LoggerContextVO lcvo = event.getLoggerContextVO();
      Map<String, String> map = lcvo.getPropertyMap();
      String val = map.get(key);
      if (val != null) {
        return val;
      } else {
        return System.getProperty(key);
      }
    }
  }
}
