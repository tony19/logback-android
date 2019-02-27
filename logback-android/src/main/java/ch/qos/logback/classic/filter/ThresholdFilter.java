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
package ch.qos.logback.classic.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filters events below the threshold level.
 * 
 * Events with a level below the specified
 * level will be denied, while events with a level
 * equal or above the specified level will trigger a
 * FilterReply.NEUTRAL result, to allow the rest of the
 * filter chain process the event.
 * 
 * For more information about filters, please refer to the online manual at
 * http://logback.qos.ch/manual/filters.html#thresholdFilter
 *
 * @author S&eacute;bastien Pennec
 */
public class ThresholdFilter extends Filter<ILoggingEvent> {

  Level level;
  
  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }
    
    if (event.getLevel().isGreaterOrEqual(level)) {
      return FilterReply.NEUTRAL;
    } else {
      return FilterReply.DENY;
    }
  }
  
  public void setLevel(String level) {
    this.level = Level.toLevel(level);
  }
  
  public void start() {
    if (this.level != null) {
      super.start();
    }
  }
}
