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
package ch.qos.logback.classic.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.SyslogConstants;

public class LevelToSyslogSeverity {

  /*
   * Convert a level to equivalent syslog severity. Only levels for printing
   * methods i.e TRACE, DEBUG, WARN, INFO and ERROR are converted.
   * 
   */
  static public int convert(ILoggingEvent event) {

    Level level = event.getLevel();

    switch (level.levelInt) {
    case Level.ERROR_INT:
      return SyslogConstants.ERROR_SEVERITY;
    case Level.WARN_INT:
      return SyslogConstants.WARNING_SEVERITY;
    case Level.INFO_INT:
      return SyslogConstants.INFO_SEVERITY;
    case Level.DEBUG_INT:
    case Level.TRACE_INT:
      return SyslogConstants.DEBUG_SEVERITY;
    default:
      throw new IllegalArgumentException("Level " + level
          + " is not a valid level for a printing method");
    }
  }
}
