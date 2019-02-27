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
package ch.qos.logback.classic.net.testObjectBuilders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class LoggingEventWithParametersBuilder implements Builder<LoggingEvent> {

  final String MSG = "aaaaabbbbbcccc {} cdddddaaaaabbbbbcccccdddddaaaa {}";

  LoggerContext loggerContext = new LoggerContext();
  private Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

  public LoggingEvent build(int i) {

    LoggingEvent le = new LoggingEvent();
    le.setTimeStamp(System.currentTimeMillis());

    Object[] aa = new Object[] { i, "HELLO WORLD [========== ]" + i };

    le.setArgumentArray(aa);
    String msg = MSG + i;
    le.setMessage(msg);

    // compute formatted message
    // this forces le.formmatedMessage to be set (this is the whole point of the
    // exercise)
    le.getFormattedMessage();
    le.setLevel(Level.DEBUG);
    le.setLoggerName(logger.getName());
    le.setLoggerContextRemoteView(loggerContext.getLoggerContextRemoteView());
    le.setThreadName("threadName");
    return le;
  }
}
