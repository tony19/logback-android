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

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

import static ch.qos.logback.core.util.OptionHelper.extractDefaultReplacement;

public class MDCConverter extends ClassicConverter {

  private String key;
  private String defaultValue = "";

  @Override
  public void start() {
		String[] keyInfo = extractDefaultReplacement(getFirstOption());
		key = keyInfo[0];
    if (keyInfo[1] != null) {
      defaultValue = keyInfo[1];
    }
    super.start();
  }

  @Override
  public void stop() {
    key = null;
    super.stop();
  }

  @Override
  public String convert(ILoggingEvent event) {
    Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

    if (mdcPropertyMap == null) {
      return defaultValue;
    }

    if (key == null) {
      return outputMDCForAllKeys(mdcPropertyMap);
    } else {

      String value = mdcPropertyMap.get(key);
      if (value != null) {
        return value;
      } else {
        return defaultValue;
      }
    }
  }

  /**
   * if no key is specified, return all the values present in the MDC, in the format "k1=v1, k2=v2, ..."
   */
  private String outputMDCForAllKeys(Map<String, String> mdcPropertyMap) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
      if (first) {
        first = false;
      } else {
        buf.append(", ");
      }
      //format: key0=value0, key1=value1
      buf.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return buf.toString();
  }
}
