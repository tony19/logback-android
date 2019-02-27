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
package ch.qos.logback.classic.jul;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @deprecated
 */
@Deprecated
public class JULHelper {


  static public final boolean isRegularNonRootLogger(java.util.logging.Logger julLogger) {
    if (julLogger == null)
      return false;
    return !julLogger.getName().equals("");
  }

  static public final boolean isRoot(java.util.logging.Logger julLogger) {
    if (julLogger == null)
      return false;
    return julLogger.getName().equals("");
  }

  static public java.util.logging.Level asJULLevel(Level lbLevel) {
    if (lbLevel == null)
      throw new IllegalArgumentException("Unexpected level [null]");

    switch (lbLevel.levelInt) {
      case Level.ALL_INT:
        return java.util.logging.Level.ALL;
      case Level.TRACE_INT:
        return java.util.logging.Level.FINEST;
      case Level.DEBUG_INT:
        return java.util.logging.Level.FINE;
      case Level.INFO_INT:
        return java.util.logging.Level.INFO;
      case Level.WARN_INT:
        return java.util.logging.Level.WARNING;
      case Level.ERROR_INT:
        return java.util.logging.Level.SEVERE;
      case Level.OFF_INT:
        return java.util.logging.Level.OFF;
      default:
        throw new IllegalArgumentException("Unexpected level [" + lbLevel + "]");
    }
  }

  static public String asJULLoggerName(String loggerName) {
    if (Logger.ROOT_LOGGER_NAME.equals(loggerName))
      return "";
    else
      return loggerName;
  }

  static public java.util.logging.Logger asJULLogger(String loggerName) {
    String julLoggerName = asJULLoggerName(loggerName);
    return java.util.logging.Logger.getLogger(julLoggerName);
  }

  static public java.util.logging.Logger asJULLogger(Logger logger) {
    return asJULLogger(logger.getName());
  }

}


