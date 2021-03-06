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

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.net.SyslogConstants;

public class LevelToSyslogSeverityTest {

  @Test
  public void smoke() {

    assertEquals(SyslogConstants.DEBUG_SEVERITY, LevelToSyslogSeverity
        .convert(createEventOfLevel(Level.TRACE)));

    assertEquals(SyslogConstants.DEBUG_SEVERITY, LevelToSyslogSeverity
        .convert(createEventOfLevel(Level.DEBUG)));

    assertEquals(SyslogConstants.INFO_SEVERITY, LevelToSyslogSeverity
        .convert(createEventOfLevel(Level.INFO)));

    assertEquals(SyslogConstants.WARNING_SEVERITY, LevelToSyslogSeverity
        .convert(createEventOfLevel(Level.WARN)));

    assertEquals(SyslogConstants.ERROR_SEVERITY, LevelToSyslogSeverity
        .convert(createEventOfLevel(Level.ERROR)));

  }

  ILoggingEvent createEventOfLevel(Level level) {
    LoggingEvent event = new LoggingEvent();
    event.setLevel(level);
    return event;
  }

}
