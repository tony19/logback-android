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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;



/**
 * Always returns an empty string.
 * <p>
 * This converter is useful to pretend that the converter chain for
 * PatternLayout actually handles exceptions, when in fact it does not.
 * By adding %nopex to the conversion pattern, the user can bypass
 * the automatic addition of %ex conversion pattern for patterns 
 * which do not contain a converter handling exceptions.
 * 
 * <p>Users can ignore the existence of this converter, unless they
 * want to suppress the automatic printing of exceptions by 
 * {@link PatternLayout}.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class NopThrowableInformationConverter extends ThrowableHandlingConverter {

  public String convert(ILoggingEvent event) {
    return CoreConstants.EMPTY_STRING;
  }
 
}
