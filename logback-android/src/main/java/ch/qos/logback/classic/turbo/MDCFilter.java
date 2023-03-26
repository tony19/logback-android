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
package ch.qos.logback.classic.turbo;

import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

/**
 * This class allows output for a given MDC value.
 * 
 * <p>
 * When the given value is identified by this TurboFilter, 
 * the reply is based on the OnMatch option.
 * The information is taken from the MDC. For this TurboFilter to work,
 * one must set the key that will be used to 
 * access the information in the MDC.
 * 
 * <p>
 * To allow output for the value, set the OnMatch option
 * to ACCEPT. To disable output for the given value, set
 * the OnMatch option to DENY.
 * 
 * <p>
 * By default, values of the OnMatch and OnMisMatch
 * options are set to NEUTRAL.
 * 
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class MDCFilter extends MatchingFilter {

  String MDCKey;
  String value;

  @Override
  public void start() {
    int errorCount = 0;
    if (value == null) {
      addError("\'value\' parameter is mandatory. Cannot start.");
      errorCount++;
    }
    if (MDCKey == null) {
      addError("\'MDCKey\' parameter is mandatory. Cannot start.");
      errorCount++;
    }

    if (errorCount == 0)
      this.start = true;
  }

  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {

    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    String value = MDC.get(MDCKey);
    if (this.value.equals(value)) {
      return onMatch;
    }
    return onMismatch;
  }

  public void setValue(String value) {
    this.value = value;
  }
  
  public void setMDCKey(String MDCKey) {
    this.MDCKey = MDCKey;
  }

}
