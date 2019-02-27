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
package ch.qos.logback.classic.sift;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import ch.qos.logback.core.util.OptionHelper;

import java.util.Map;

/**
 * MDCBasedDiscriminator essentially returns the value mapped to an MDC key. If
 * the said value is null, then a default value is returned.
 * <p>
 * <p>Both Key and the DefaultValue are user specified properties.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class MDCBasedDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

  private String key;
  private String defaultValue;

  /**
   * Return the value associated with an MDC entry designated by the Key
   * property. If that value is null, then return the value assigned to the
   * DefaultValue property.
   */
  public String getDiscriminatingValue(ILoggingEvent event) {
    // http://jira.qos.ch/browse/LBCLASSIC-213
    Map<String, String> mdcMap = event.getMDCPropertyMap();
    if (mdcMap == null) {
      return defaultValue;
    }
    String mdcValue = mdcMap.get(key);
    if (mdcValue == null) {
      return defaultValue;
    } else {
      return mdcValue;
    }
  }

  @Override
  public void start() {
    int errors = 0;
    if (OptionHelper.isEmpty(key)) {
      errors++;
      addError("The \"Key\" property must be set");
    }
    if (OptionHelper.isEmpty(defaultValue)) {
      errors++;
      addError("The \"DefaultValue\" property must be set");
    }
    if (errors == 0) {
      started = true;
    }
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return The default MDC value in case the MDC is not set for
   * {@link #setKey(String) mdcKey}.
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * The default MDC value in case the MDC is not set for
   * {@link #setKey(String) mdcKey}.
   * <p>
   * <p> For example, if {@link #setKey(String) Key} is set to the value
   * "someKey", and the MDC is not set for "someKey", then this appender will
   * use the default value, which you can set with the help of this method.
   *
   * @param defaultValue
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
