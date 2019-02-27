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
package org.dummy;

import static junit.framework.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Used to test log4j-over-slf4j
 * 
 * @author Ceki Gulcu
 *
 */
@RunWith(RobolectricTestRunner.class)
public class Log4jInvocation {

  static final String HELLO = "Hello";

  DummyLBAppender listAppender;
  LoggerContext lc;
  ch.qos.logback.classic.Logger rootLogger;
  
  @Before
  public void fixture() {
    lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();

    listAppender = new DummyLBAppender();
    listAppender.setContext(lc);
    listAppender.start();
    rootLogger = lc.getLogger("root");
    rootLogger.addAppender(listAppender);
  }

  @Test
  public void basic() {
    assertEquals(0, listAppender.list.size());

    Logger logger = Logger.getLogger("basic-test");
    logger.debug(HELLO);

    assertEquals(1, listAppender.list.size());
    ILoggingEvent event = (ILoggingEvent) listAppender.list.get(0);
    assertEquals(HELLO, event.getMessage());
  }

  @Test
  public void callerData() {
    assertEquals(0, listAppender.list.size());

    PatternLayout pl = new PatternLayout();
    pl.setPattern("%-5level [%class] %logger - %msg");
    pl.setContext(lc);
    pl.start();
    listAppender.layout = pl;

    Logger logger = Logger.getLogger("basic-test");
    logger.trace("none");
    assertEquals(0, listAppender.list.size());
    
    rootLogger.setLevel(Level.TRACE);
    logger.trace(HELLO);
    assertEquals(1, listAppender.list.size());

    ILoggingEvent event = (ILoggingEvent) listAppender.list.get(0);
    assertEquals(HELLO, event.getMessage());

    assertEquals(1, listAppender.stringList.size());
    assertEquals("TRACE [" + Log4jInvocation.class.getName()
        + "] basic-test - Hello", listAppender.stringList.get(0));
  }
}
