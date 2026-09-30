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
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.turbo.NOPTurboFilter;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.status.StatusManager;

public class LoggerContextTest {
  LoggerContext lc;

  @Before
  public void setUp() throws Exception {
    lc = new LoggerContext();
    lc.setName("x");
  }

  @Test
  public void testRootGetLogger() {
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    assertEquals(Level.DEBUG, root.getLevel());
    assertEquals(Level.DEBUG, root.getEffectiveLevel());
  }

  @Test
  public void testLoggerX() {
    Logger x = lc.getLogger("x");
    assertNotNull(x);
    assertEquals("x", x.getName());
    assertNull(x.getLevel());
    assertEquals(Level.DEBUG, x.getEffectiveLevel());
  }

  @Test
  public void testNull() {
    try {
      lc.getLogger((String) null);
      fail("null should cause an exception");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testEmpty() {
    Logger empty = lc.getLogger("");
    LoggerTestHelper.assertNameEquals(empty, "");
    LoggerTestHelper.assertLevels(null, empty, Level.DEBUG);

    Logger dot = lc.getLogger(".");
    LoggerTestHelper.assertNameEquals(dot, ".");
    // LoggerTestHelper.assertNameEquals(dot.parent, "");
    // LoggerTestHelper.assertNameEquals(dot.parent.parent, "root");

    // assertNull(dot.parent.parent.parent);
    LoggerTestHelper.assertLevels(null, dot, Level.DEBUG);


    assertEquals(3, lc.getLoggerList().size());
  }

  @Test
  public void testDotDot() {
    Logger dotdot = lc.getLogger("..");
    assertEquals(4, lc.getLoggerList().size());
    LoggerTestHelper.assertNameEquals(dotdot, "..");
    // LoggerTestHelper.assertNameEquals(dotdot.parent, ".");
    // LoggerTestHelper.assertNameEquals(dotdot.parent.parent, "");
    // LoggerTestHelper.assertNameEquals(dotdot.parent.parent.parent, "root");
  }

  int instanceCount() {
    return lc.getLoggerList().size();
  }

  @Test
  public void testLoggerXY() {
    assertEquals(1, lc.getLoggerList().size());

    Logger xy = lc.getLogger("x.y");
    assertEquals(3, instanceCount());
    LoggerTestHelper.assertNameEquals(xy, "x.y");
    LoggerTestHelper.assertLevels(null, xy, Level.DEBUG);

    Logger x = lc.getLogger("x");
    assertEquals(3, instanceCount());

    Logger xy2 = lc.getLogger("x.y");
    assertEquals(xy, xy2);

    Logger x2 = lc.getLogger("x");
    assertEquals(x, x2);
    assertEquals(3, instanceCount());
  }

  @Test
  public void testLoggerMultipleChildren() {
    assertEquals(1, instanceCount());
    Logger xy0 = lc.getLogger("x.y0");
    LoggerTestHelper.assertNameEquals(xy0, "x.y0");

    Logger xy1 = lc.getLogger("x.y1");
    LoggerTestHelper.assertNameEquals(xy1, "x.y1");

    LoggerTestHelper.assertLevels(null, xy0, Level.DEBUG);
    LoggerTestHelper.assertLevels(null, xy1, Level.DEBUG);
    assertEquals(4, instanceCount());

    for (int i = 0; i < 100; i++) {
      Logger xy_i = lc.getLogger("x.y" + i);
      LoggerTestHelper.assertNameEquals(xy_i, "x.y" + i);
      LoggerTestHelper.assertLevels(null, xy_i, Level.DEBUG);
    }
    assertEquals(102, instanceCount());
  }

  @Test
  public void testMultiLevel() {
    Logger wxyz = lc.getLogger("w.x.y.z");
    LoggerTestHelper.assertNameEquals(wxyz, "w.x.y.z");
    LoggerTestHelper.assertLevels(null, wxyz, Level.DEBUG);

    Logger wx = lc.getLogger("w.x");
    wx.setLevel(Level.INFO);
    LoggerTestHelper.assertNameEquals(wx, "w.x");
    LoggerTestHelper.assertLevels(Level.INFO, wx, Level.INFO);
    LoggerTestHelper.assertLevels(null, lc.getLogger("w.x.y"), Level.INFO);
    LoggerTestHelper.assertLevels(null, wxyz, Level.INFO);
  }

  @Test
  public void testStatusWithUnconfiguredContext() {
    Logger logger = lc.getLogger(LoggerContextTest.class);

    for (int i = 0; i < 3; i++) {
      logger.debug("test");
    }

    logger = lc.getLogger("x.y.z");

    for (int i = 0; i < 3; i++) {
      logger.debug("test");
    }

    StatusManager sm = lc.getStatusManager();
    assertTrue("StatusManager has recieved too many messages",
            sm.getCount() == 1);
  }


  @Test
  public void resetTest() {

    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    Logger a = lc.getLogger("a");
    Logger ab = lc.getLogger("a.b");

    ab.setLevel(Level.WARN);
    root.setLevel(Level.INFO);
    lc.reset();
    assertEquals(Level.DEBUG, root.getEffectiveLevel());
    assertTrue(root.isDebugEnabled());
    assertEquals(Level.DEBUG, a.getEffectiveLevel());
    assertEquals(Level.DEBUG, ab.getEffectiveLevel());

    assertEquals(Level.DEBUG, root.getLevel());
    assertNull(a.getLevel());
    assertNull(ab.getLevel());
  }

  // http://jira.qos.ch/browse/LBCLASSIC-89
  @Test
  public void turboFilterStopOnReset() {
    NOPTurboFilter nopTF = new NOPTurboFilter();
    nopTF.start();
    lc.addTurboFilter(nopTF);
    assertTrue(nopTF.isStarted());
    lc.reset();
    assertFalse(nopTF.isStarted());
  }

  @Test
  public void resetTest_LBCORE_104() {
    lc.putProperty("keyA", "valA");
    lc.putObject("keyA", "valA");
    assertEquals("valA", lc.getProperty("keyA"));
    assertEquals("valA", lc.getObject("keyA"));
    lc.reset();
    assertNull(lc.getProperty("keyA"));
    assertNull(lc.getObject("keyA"));
  }

  @Test
  public void loggerNameEndingInDotOrDollarShouldWork() {
    {
      String loggerName = "toto.x.";
      Logger logger = lc.getLogger(loggerName);
      assertEquals(loggerName, logger.getName());
    }

    {
      String loggerName = "toto.x$";
      Logger logger = lc.getLogger(loggerName);
      assertEquals(loggerName, logger.getName());
    }
  }

  @Test
  public void levelResetTest() {
    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.TRACE);
    assertTrue(root.isTraceEnabled());
    lc.reset();
    assertFalse(root.isTraceEnabled());
    assertTrue(root.isDebugEnabled());
  }

  @Test
  public void evaluatorMapPostReset() {
    lc.reset();
    assertNotNull(lc.getObject(CoreConstants.EVALUATOR_MAP));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void collisionMapsPostReset() {
    lc.reset();

    Map<String, String> fileCollisions = (Map<String, String>) lc.getObject(CoreConstants.FA_FILENAME_COLLISION_MAP);
    assertNotNull(fileCollisions);
    assertTrue(fileCollisions.isEmpty());

    Map<String, FileNamePattern> filenamePatternCollisionMap = (Map<String, FileNamePattern>) lc.getObject(CoreConstants.RFA_FILENAME_PATTERN_COLLISION_MAP);
    assertNotNull(filenamePatternCollisionMap);
    assertTrue(filenamePatternCollisionMap.isEmpty());

  }

  // http://jira.qos.ch/browse/LOGBACK-142
  @Test
  public void concurrentModification() {
    final int runLen = 100;
    Thread thread = new Thread(new Runnable() {
      public void run() {
        for (int i = 0; i < runLen; i++)  {
          lc.getLogger("a" + i);
          Thread.yield();
        }
      }
    });
    thread.start();

    for (int i = 0; i < runLen; i++) {
      lc.putProperty("a" + i, "val");
      Thread.yield();
    }

  }

  @Test
  public void putPropertiesAddsEveryPropertyAndRefreshesRemoteView() {
    LoggerContextVO before = lc.getLoggerContextRemoteView();
    Properties props = new Properties();
    props.setProperty("k1", "v1");
    props.setProperty("k2", "v2");

    lc.putProperties(props);

    assertEquals("v1", lc.getProperty("k1"));
    assertEquals("v2", lc.getProperty("k2"));
    LoggerContextVO after = lc.getLoggerContextRemoteView();
    assertNotSame(before, after);
    assertEquals("v1", after.getPropertyMap().get("k1"));
    assertEquals("v2", after.getPropertyMap().get("k2"));
    assertNull(before.getPropertyMap().get("k1"));
  }

  @Test
  public void oneArgumentLogCallIsDecidedByTurboFiltersWithThatArgument() {
    RecordingTurboFilter tf = addRecordingTurboFilter(FilterReply.ACCEPT);
    ListAppender<ILoggingEvent> la = attachListAppender();
    Logger logger = lc.getLogger("one");
    logger.setLevel(Level.ERROR);

    logger.debug("a {}", "p1");

    assertEquals("a {}", tf.format);
    assertEquals(Level.DEBUG, tf.level);
    assertArrayEquals(new Object[] {"p1"}, tf.params);
    // ACCEPT overrides the logger level
    assertEquals(1, la.list.size());
    assertEquals("a p1", la.list.get(0).getFormattedMessage());

    tf.reply = FilterReply.DENY;
    logger.error("b {}", "p2");
    assertArrayEquals(new Object[] {"p2"}, tf.params);
    assertEquals(1, la.list.size());
  }

  @Test
  public void twoArgumentLogCallIsDecidedByTurboFiltersWithBothArguments() {
    RecordingTurboFilter tf = addRecordingTurboFilter(FilterReply.ACCEPT);
    ListAppender<ILoggingEvent> la = attachListAppender();
    Logger logger = lc.getLogger("two");
    logger.setLevel(Level.ERROR);

    logger.info("a {} {}", "p1", "p2");

    assertEquals("a {} {}", tf.format);
    assertEquals(Level.INFO, tf.level);
    assertArrayEquals(new Object[] {"p1", "p2"}, tf.params);
    assertEquals(1, la.list.size());
    assertEquals("a p1 p2", la.list.get(0).getFormattedMessage());

    tf.reply = FilterReply.DENY;
    logger.error("b {} {}", "p3", "p4");
    assertArrayEquals(new Object[] {"p3", "p4"}, tf.params);
    assertEquals(1, la.list.size());
  }

  @Test
  public void removedListenerIsNoLongerNotified() {
    RecordingListener kept = new RecordingListener();
    RecordingListener removed = new RecordingListener();
    lc.addListener(kept);
    lc.addListener(removed);
    assertEquals(2, lc.getCopyOfListenerList().size());

    lc.removeListener(removed);
    lc.start();

    List<LoggerContextListener> listeners = lc.getCopyOfListenerList();
    assertEquals(1, listeners.size());
    assertTrue(listeners.contains(kept));
    assertEquals(1, kept.startCount);
    assertEquals(0, removed.startCount);
  }

  @Test
  public void copyOfListenerListIsDetachedFromTheContext() {
    RecordingListener listener = new RecordingListener();
    lc.addListener(listener);

    List<LoggerContextListener> copy = lc.getCopyOfListenerList();
    copy.clear();

    assertEquals(1, lc.getCopyOfListenerList().size());
    lc.start();
    assertEquals(1, listener.startCount);
  }

  @Test
  public void maxCallerDataDepthLimitsExtractedCallerData() {
    assertEquals(ClassicConstants.DEFAULT_MAX_CALLEDER_DATA_DEPTH, lc.getMaxCallerDataDepth());
    lc.setMaxCallerDataDepth(3);
    assertEquals(3, lc.getMaxCallerDataDepth());

    LoggingEvent event = new LoggingEvent(LoggerContextTest.class.getName(), lc.getLogger("x"), Level.INFO, "m", null, null);
    // the caller is the frame right below this test class, and the stack below it is deeper than 3
    assertEquals(3, event.getCallerData().length);
  }

  private RecordingTurboFilter addRecordingTurboFilter(FilterReply reply) {
    RecordingTurboFilter tf = new RecordingTurboFilter();
    tf.reply = reply;
    tf.start();
    lc.addTurboFilter(tf);
    return tf;
  }

  private ListAppender<ILoggingEvent> attachListAppender() {
    ListAppender<ILoggingEvent> la = new ListAppender<ILoggingEvent>();
    la.setContext(lc);
    la.start();
    lc.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(la);
    return la;
  }

  static class RecordingTurboFilter extends TurboFilter {
    FilterReply reply;
    String format;
    Level level;
    Object[] params;

    @Override
    public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {
      this.level = level;
      this.format = format;
      this.params = params;
      return reply;
    }
  }

  static class RecordingListener implements LoggerContextListener {
    int startCount;

    public boolean isResetResistant() {
      return false;
    }

    public void onStart(LoggerContext context) {
      startCount++;
    }

    public void onReset(LoggerContext context) {
    }

    public void onStop(LoggerContext context) {
    }

    public void onLevelChange(Logger logger, Level level) {
    }
  }
}