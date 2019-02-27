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

/**
 * This discriminator returns the value context to which this event is attached
 * to. If the said value is null, then a default value is returned.
 * 
 * <p>
 * Both Key and the DefaultValue are user specified properties.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 */
public class ContextBasedDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

  private static final String KEY = "contextName";
  private String defaultValue;

  /**
   * Return the name of the current context name as found in the logging event.
   */
  public String getDiscriminatingValue(ILoggingEvent event) {
    String contextName = event.getLoggerContextVO().getName();

    if (contextName == null) {
      return defaultValue;
    } else {
      return contextName;
    }
  }

  public String getKey() {
    return KEY;
  }

  public void setKey(String key) {
    throw new UnsupportedOperationException(
        "Key cannot be set. Using fixed key " + KEY);
  }

  /**
   * @return The default context name in case the context name is not set for the
   * current logging event.
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * The default context name in case the context name is not set for the
   * current logging event.
   * 
   * @param defaultValue desired default value
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
