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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;

@RunWith(RobolectricTestRunner.class)
public class LoggerTest {

  LoggerContext lc = new LoggerContext();
  Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
  Logger loggerTest = lc.getLogger(LoggerTest.class);

  ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>();

  @Test
  public void smoke() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    root.addAppender(listAppender);
    Logger logger = lc.getLogger(LoggerTest.class);
    assertEquals(0, listAppender.list.size());
    logger.debug("hello");
    assertEquals(1, listAppender.list.size());
  }

  @Test
  public void testNoStart() {
    // listAppender.start();
    listAppender.setContext(lc);
    root.addAppender(listAppender);
    Logger logger = lc.getLogger(LoggerTest.class);
    logger.debug("hello");

    List<Status> statusList = lc.getStatusManager().getCopyOfStatusList();
    Status s0 = statusList.get(0);
    assertEquals(Status.WARN, s0.getLevel());
    assertTrue(s0.getMessage().startsWith("Attempted to append to non started"));
  }

  @Test
  public void testAdditive() {
    listAppender.start();
    root.addAppender(listAppender);
    loggerTest.addAppender(listAppender);
    loggerTest.setAdditive(false);
    loggerTest.debug("hello");
    // 1 instead of two, since logger is not additive
    assertEquals(1, listAppender.list.size());
  }

  @Test
  public void testRootLogger() {
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    LoggerContext lc = logger.getLoggerContext();

    assertNotNull("Returned logger is null", logger);
    assertEquals("Return logger isn't named root", logger.getName(),
        Logger.ROOT_LOGGER_NAME);
    assertTrue("logger instances should be indentical", logger == lc.root);
  }

  @Test
  public void testBasicFiltering() throws Exception {
    listAppender.start();
    root.addAppender(listAppender);
    root.setLevel(Level.INFO);
    loggerTest.debug("x");
    assertEquals(0, listAppender.list.size());
    loggerTest.info("x");
    loggerTest.warn("x");
    loggerTest.error("x");
    assertEquals(3, listAppender.list.size());
  }

  void checkLevelThreshold(Logger logger, Level threshold) {

    if (Level.ERROR_INT >= threshold.levelInt) {
      assertTrue(logger.isErrorEnabled());
      assertTrue(logger.isEnabledFor(Level.ERROR));
    } else {
      assertFalse(logger.isErrorEnabled());
      assertFalse(logger.isEnabledFor(Level.ERROR));
    }

    if (Level.WARN_INT >= threshold.levelInt) {
      assertTrue(logger.isWarnEnabled());
      assertTrue(logger.isEnabledFor(Level.WARN));
    } else {
      assertFalse(logger.isWarnEnabled());
      assertFalse(logger.isEnabledFor(Level.WARN));
    }
    if (Level.INFO_INT >= threshold.levelInt) {
      assertTrue(logger.isInfoEnabled());
      assertTrue(logger.isEnabledFor(Level.INFO));
    } else {
      assertFalse(logger.isInfoEnabled());
      assertFalse(logger.isEnabledFor(Level.INFO));
    }
    if (Level.DEBUG_INT >= threshold.levelInt) {
      assertTrue(logger.isDebugEnabled());
      assertTrue(logger.isEnabledFor(Level.DEBUG));
    } else {
      assertFalse(logger.isDebugEnabled());
      assertFalse(logger.isEnabledFor(Level.DEBUG));
    }
    if (Level.TRACE_INT >= threshold.levelInt) {
      assertTrue(logger.isTraceEnabled());
      assertTrue(logger.isEnabledFor(Level.TRACE));
    } else {
      assertFalse(logger.isTraceEnabled());
      assertFalse(logger.isEnabledFor(Level.TRACE));
    }
  }

  @Test
  public void  innerClass_I() {
    root.setLevel(Level.DEBUG);
    Logger a = lc.getLogger("a");
    a.setLevel(Level.INFO);
    Logger a_b = lc.getLogger("a$b");
    assertEquals(Level.INFO, a_b.getEffectiveLevel());
  }

  @Test
  public void  innerClass_II() {
    root.setLevel(Level.DEBUG);
    Logger a = lc.getLogger(this.getClass());
    a.setLevel(Level.INFO);
    Logger a_b = lc.getLogger(new Inner().getClass());
    assertEquals(Level.INFO, a_b.getEffectiveLevel());
  }

  
  class Inner {
  }
  
  @Test
  public void testEnabled_All() throws Exception {
    root.setLevel(Level.ALL);
    checkLevelThreshold(loggerTest, Level.ALL);
  }

  @Test
  public void testEnabled_Debug() throws Exception {
    root.setLevel(Level.DEBUG);
    checkLevelThreshold(loggerTest, Level.DEBUG);
  }

  @Test
  public void testEnabled_Info() throws Exception {
    root.setLevel(Level.INFO);
    checkLevelThreshold(loggerTest, Level.INFO);
  }

  @Test
  public void testEnabledX_Warn() throws Exception {
    root.setLevel(Level.WARN);
    checkLevelThreshold(loggerTest, Level.WARN);
  }

  public void testEnabledX_Errror() throws Exception {
    root.setLevel(Level.ERROR);
    checkLevelThreshold(loggerTest, Level.ERROR);
  }

  @Test
  public void testEnabledX_Off() throws Exception {
    root.setLevel(Level.OFF);
    checkLevelThreshold(loggerTest, Level.OFF);
  }

  @Test
  public void setRootLevelToNull() {
    try {
      root.setLevel(null);
      fail("The level of the root logger should not be settable to null");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void setLevelToNull_A() {
    loggerTest.setLevel(null);
    assertEquals(root.getEffectiveLevel(), loggerTest.getEffectiveLevel());
  }
  
  @Test
  public void setLevelToNull_B() {
    loggerTest.setLevel(Level.DEBUG);
    loggerTest.setLevel(null);
    assertEquals(root.getEffectiveLevel(), loggerTest.getEffectiveLevel());
  }
  
  @Test
  public void setLevelToNull_LBCLASSIC_91() {
    loggerTest.setLevel(Level.DEBUG);
    ch.qos.logback.classic.Logger child = lc.getLogger(loggerTest.getName() + ".child");
    loggerTest.setLevel(null);
    assertEquals(root.getEffectiveLevel(), loggerTest.getEffectiveLevel());
    assertEquals(root.getEffectiveLevel(), child.getEffectiveLevel());
  }

}
