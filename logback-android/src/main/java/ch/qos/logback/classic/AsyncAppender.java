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
package ch.qos.logback.classic;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;

/**
 * In order to optimize performance this appender deems events of level TRACE, DEBUG and INFO as discardable. See the
 * <a href="http://logback.qos.ch/manual/appenders.html#AsyncAppender">chapter on appenders</a> in the manual for
 * further information.
 *
 *
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.0.4
 */
public class AsyncAppender extends AsyncAppenderBase<ILoggingEvent> {

  boolean includeCallerData = false;


  /**
   * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
   * @param event
   * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
   */
  protected boolean isDiscardable(ILoggingEvent event) {
    Level level = event.getLevel();
    return level.toInt() <= Level.INFO_INT;
  }

  protected void preprocess(ILoggingEvent eventObject) {
    eventObject.prepareForDeferredProcessing();
    if(includeCallerData)
      eventObject.getCallerData();
  }

  public boolean isIncludeCallerData() {
    return includeCallerData;
  }

  public void setIncludeCallerData(boolean includeCallerData) {
    this.includeCallerData = includeCallerData;
  }

}
