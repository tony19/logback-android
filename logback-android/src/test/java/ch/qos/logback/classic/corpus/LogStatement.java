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
package ch.qos.logback.classic.corpus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.IThrowableProxy;

/**
 * Captures the data contained within a log statement, that is the data that the
 * developer puts in the source code when he writes:
 * 
 * <p>logger.debug("hello world");
 * 
 * @author Ceki G&uuml;lc&uuml; 
 */
public class LogStatement {

  final String loggerName;
  final MessageArgumentTuple mat;
  final Level level;
  final IThrowableProxy throwableProxy;

  public LogStatement(String loggerName, Level level, MessageArgumentTuple mat,
      IThrowableProxy tp) {
    this.loggerName = loggerName;
    this.level = level;
    this.mat = mat;
    this.throwableProxy = tp;
  }


  public String getLoggerName() {
    return loggerName;
  }
}
