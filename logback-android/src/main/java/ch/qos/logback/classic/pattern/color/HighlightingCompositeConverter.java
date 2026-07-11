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
package ch.qos.logback.classic.pattern.color;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * Colors the child output according to the event's level: bold red for
 * ERROR, red for WARN, blue for INFO, and the default color otherwise
 * (issue #332). Used via {@code %highlight(...)}.
 */
public class HighlightingCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

  @Override
  protected String getForegroundColorCode(ILoggingEvent event) {
    Level level = event.getLevel();
    switch (level.toInt()) {
      case Level.ERROR_INT:
        return ANSIConstants.BOLD + ANSIConstants.RED_FG;
      case Level.WARN_INT:
        return ANSIConstants.RED_FG;
      case Level.INFO_INT:
        return ANSIConstants.BLUE_FG;
      default:
        return ANSIConstants.DEFAULT_FG;
    }
  }
}
