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
package ch.qos.logback.classic.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class LoggingEventTest {

  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

  @Before
  public void setUp() {
  }


  @Test
  public void testFormattingOneArg() {
    String message = "x={}";
    Throwable throwable = null;
    Object[] argArray = new Object[] {12};

    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, message, throwable, argArray);
    assertNull(event.formattedMessage);
    assertEquals("x=12", event.getFormattedMessage());
  }


  @Test
  public void testFormattingTwoArg() {
    String message = "{}-{}";
    Throwable throwable = null;
    Object[] argArray = new Object[] {12, 13};
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, message, throwable, argArray);

    assertNull(event.formattedMessage);
    assertEquals("12-13", event.getFormattedMessage());
  }


  @Test
  public void testNoFormattingWithArgs() {
    String message = "testNoFormatting";
    Throwable throwable = null;
    Object[] argArray = new Object[] {12, 13};
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, message, throwable, argArray);
    assertNull(event.formattedMessage);
    assertEquals(message, event.getFormattedMessage());
  }

  @Test
  public void testNoFormattingWithoutArgs() {
    String message = "testNoFormatting";
    Throwable throwable = null;
    Object[] argArray = null;
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, message, throwable, argArray);
    assertNull(event.formattedMessage);
    assertEquals(message, event.getFormattedMessage());
  }
}
