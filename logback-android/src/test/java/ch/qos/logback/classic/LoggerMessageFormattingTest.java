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

import static junit.framework.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class LoggerMessageFormattingTest {

  LoggerContext lc;
  ListAppender<ILoggingEvent> listAppender;

  @Before
  public void setUp() {
    lc = new LoggerContext();
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    listAppender = new ListAppender<ILoggingEvent>();
    listAppender.setContext(lc);
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @Test
  public void testFormattingOneArg() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.debug("{}", 12);
    ILoggingEvent event = listAppender.list.get(0);
    assertEquals("12", event.getFormattedMessage());
  }

  @Test
  public void testFormattingTwoArg() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.debug("{}-{}", 12, 13);
    ILoggingEvent event = listAppender.list.get(0);
    assertEquals("12-13", event.getFormattedMessage());
  }

  @Test
  public void testNoFormatting() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.debug("test", 12, 13);
    ILoggingEvent event = listAppender.list.get(0);
    assertEquals("test", event.getFormattedMessage());
  }

  @Test
  public void testNoFormatting2() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.debug("test");
    ILoggingEvent event = listAppender.list.get(0);
    assertEquals("test", event.getFormattedMessage());
  }

  @Test
  public void testMessageConverter() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.debug("{}", 12);
    ILoggingEvent event = listAppender.list.get(0);
    PatternLayout layout = new PatternLayout();
    layout.setContext(lc);
    layout.setPattern("%m");
    layout.start();
    String formattedMessage = layout.doLayout(event);
    assertEquals("12", formattedMessage);
  }



}
