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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.slf4j.Marker;
import org.slf4j.event.DefaultLoggingEvent;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.LocationAwareLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Plain-JVM tests of {@link Logger}: every printing overload, the
 * {@code isXxxEnabled} checks and how they follow turbo filter replies, the
 * {@code log} entry points, appender attachment and child creation.
 * {@link LoggerTest} covers the basics under Robolectric.
 */
public class LoggerApiTest {

  static final BasicMarkerFactory MARKER_FACTORY = new BasicMarkerFactory();
  static final Marker BLUE = MARKER_FACTORY.getDetachedMarker("BLUE");
  static final Marker RED = MARKER_FACTORY.getDetachedMarker("RED");
  static final List<Marker> MARKERS = Arrays.asList(BLUE, RED);

  static final Object A1 = "a1";
  static final Object A2 = "a2";
  static final Object A3 = "a3";
  static final Throwable EX = new Exception("boom");

  /**
   * The five argument shapes of each printing method, in the order in which
   * {@code log*Overloads} call them: message only, one argument, two
   * arguments, varargs and throwable.
   */
  static final String[] SHAPE_MESSAGES = { "m0", "m1 {}", "m2 {} {}", "m3 {} {} {}", "mt" };
  static final Object[][] SHAPE_ARGS = { null, { A1 }, { A1, A2 }, { A1, A2, A3 }, null };
  static final int THROWABLE_SHAPE = 4;

  LoggerContext lc = new LoggerContext();
  Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
  Logger logger = lc.getLogger(LoggerApiTest.class);
  CallerDataCapturingAppender appender = new CallerDataCapturingAppender();

  @Before
  public void setUp() {
    appender.setContext(lc);
    appender.setName("capture");
    appender.start();
    root.addAppender(appender);
  }

  @After
  public void tearDown() {
    lc.stop();
  }

  // ---------------------------------------------------------------- appenders

  @Test
  public void loggerWithoutAppendersReportsNoneAttached() {
    Logger bare = lc.getLogger("bare");
    ListAppender<ILoggingEvent> other = new ListAppender<ILoggingEvent>();
    other.setName("other");

    assertFalse(bare.detachAppender("other"));
    assertFalse(bare.detachAppender(other));
    assertFalse(bare.isAttached(other));
    assertNull(bare.getAppender("other"));
    assertFalse(bare.iteratorForAppenders().hasNext());
  }

  @Test
  public void appenderQueriesReflectAttachAndDetachByName() {
    Logger owner = lc.getLogger("owner");
    ListAppender<ILoggingEvent> other = new ListAppender<ILoggingEvent>();
    other.setName("other");
    owner.addAppender(other);

    assertTrue(owner.isAttached(other));
    assertFalse(owner.isAttached(appender));
    assertSame(other, owner.getAppender("other"));
    assertFalse(owner.detachAppender("unknown"));
    assertTrue(owner.detachAppender("other"));

    assertFalse(owner.isAttached(other));
    assertNull(owner.getAppender("other"));
    assertFalse(owner.detachAppender("other"));
  }

  @Test
  public void appenderQueriesReflectAttachAndDetachByInstance() {
    Logger owner = lc.getLogger("owner");
    ListAppender<ILoggingEvent> other = new ListAppender<ILoggingEvent>();
    owner.addAppender(other);

    assertFalse(owner.detachAppender(appender));
    assertTrue(owner.detachAppender(other));

    assertFalse(owner.isAttached(other));
    assertFalse(owner.iteratorForAppenders().hasNext());
    assertFalse(owner.detachAppender(other));
  }

  // ------------------------------------------------------------------ levels

  @Test
  public void effectiveLevelIntFollowsTheInheritedAndTheAssignedLevel() {
    root.setLevel(Level.WARN);
    assertEquals(Level.WARN_INT, logger.getEffectiveLevelInt());

    logger.setLevel(Level.TRACE);
    assertEquals(Level.TRACE_INT, logger.getEffectiveLevelInt());
  }

  // ---------------------------------------------------------- child creation

  @Test
  public void childCreatedByLastNamePartOfRootIsNamedByThatPartAlone() {
    root.setLevel(Level.WARN);

    Logger child = root.createChildByLastNamePart("x");

    assertEquals("x", child.getName());
    assertSame(child, root.getChildByName("x"));
    assertEquals(Level.WARN, child.getEffectiveLevel());
    assertNull(child.getLevel());
  }

  @Test
  public void childrenCreatedByLastNamePartOfNonRootLoggerArePrefixedWithItsName() {
    Logger parent = lc.getLogger("p");
    parent.setLevel(Level.ERROR);

    Logger first = parent.createChildByLastNamePart("c1");
    Logger second = parent.createChildByLastNamePart("c2");

    assertEquals("p.c1", first.getName());
    assertEquals("p.c2", second.getName());
    assertSame(first, parent.getChildByName("p.c1"));
    assertSame(second, parent.getChildByName("p.c2"));
    assertEquals(Level.ERROR, first.getEffectiveLevel());
    assertEquals(Level.ERROR, second.getEffectiveLevel());

    // the children are linked to the parent: level changes propagate
    parent.setLevel(Level.INFO);
    assertEquals(Level.INFO, first.getEffectiveLevel());
    assertEquals(Level.INFO, second.getEffectiveLevel());
  }

  @Test
  public void createChildByLastNamePartRejectsANameWithASeparator() {
    Logger parent = lc.getLogger("p");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> parent.createChildByLastNamePart("c.d"));

    assertEquals("Child name [c.d passed as parameter, may not include [.]", e.getMessage());
    assertNull(parent.getChildByName("p.c.d"));
  }

  @Test
  public void createChildByNameRejectsASeparatorAfterTheParentName() {
    Logger parent = lc.getLogger("p");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> parent.createChildByName("p.c.d"));

    assertEquals("For logger [p] child name [p.c.d passed as parameter, may not include '.' after index2",
        e.getMessage());
    assertNull(parent.getChildByName("p.c.d"));
  }

  // --------------------------------------------------------- print overloads

  @Test
  public void traceOverloadsAppendEventsWithTheirMarkersArgumentsAndThrowable() {
    logger.setLevel(Level.TRACE);

    logger.trace("m0");
    logger.trace("m1 {}", A1);
    logger.trace("m2 {} {}", A1, A2);
    logger.trace("m3 {} {} {}", A1, A2, A3);
    logger.trace("mt", EX);
    logger.trace(BLUE, "m0");
    logger.trace(BLUE, "m1 {}", A1);
    logger.trace(BLUE, "m2 {} {}", A1, A2);
    logger.trace(BLUE, "m3 {} {} {}", A1, A2, A3);
    logger.trace(BLUE, "mt", EX);
    logger.trace(MARKERS, "m0");
    logger.trace(MARKERS, "m1 {}", A1);
    logger.trace(MARKERS, "m2 {} {}", A1, A2);
    logger.trace(MARKERS, "m3 {} {} {}", A1, A2, A3);
    logger.trace(MARKERS, "mt", EX);

    assertOverloadEvents(Level.TRACE);
  }

  @Test
  public void debugOverloadsAppendEventsWithTheirMarkersArgumentsAndThrowable() {
    logger.setLevel(Level.DEBUG);

    logger.debug("m0");
    logger.debug("m1 {}", A1);
    logger.debug("m2 {} {}", A1, A2);
    logger.debug("m3 {} {} {}", A1, A2, A3);
    logger.debug("mt", EX);
    logger.debug(BLUE, "m0");
    logger.debug(BLUE, "m1 {}", A1);
    logger.debug(BLUE, "m2 {} {}", A1, A2);
    logger.debug(BLUE, "m3 {} {} {}", A1, A2, A3);
    logger.debug(BLUE, "mt", EX);
    logger.debug(MARKERS, "m0");
    logger.debug(MARKERS, "m1 {}", A1);
    logger.debug(MARKERS, "m2 {} {}", A1, A2);
    logger.debug(MARKERS, "m3 {} {} {}", A1, A2, A3);
    logger.debug(MARKERS, "mt", EX);

    assertOverloadEvents(Level.DEBUG);
  }

  @Test
  public void infoOverloadsAppendEventsWithTheirMarkersArgumentsAndThrowable() {
    logger.setLevel(Level.INFO);

    logger.info("m0");
    logger.info("m1 {}", A1);
    logger.info("m2 {} {}", A1, A2);
    logger.info("m3 {} {} {}", A1, A2, A3);
    logger.info("mt", EX);
    logger.info(BLUE, "m0");
    logger.info(BLUE, "m1 {}", A1);
    logger.info(BLUE, "m2 {} {}", A1, A2);
    logger.info(BLUE, "m3 {} {} {}", A1, A2, A3);
    logger.info(BLUE, "mt", EX);
    logger.info(MARKERS, "m0");
    logger.info(MARKERS, "m1 {}", A1);
    logger.info(MARKERS, "m2 {} {}", A1, A2);
    logger.info(MARKERS, "m3 {} {} {}", A1, A2, A3);
    logger.info(MARKERS, "mt", EX);

    assertOverloadEvents(Level.INFO);
  }

  @Test
  public void warnOverloadsAppendEventsWithTheirMarkersArgumentsAndThrowable() {
    logger.setLevel(Level.WARN);

    logger.warn("m0");
    logger.warn("m1 {}", A1);
    logger.warn("m2 {} {}", A1, A2);
    logger.warn("m3 {} {} {}", A1, A2, A3);
    logger.warn("mt", EX);
    logger.warn(BLUE, "m0");
    logger.warn(BLUE, "m1 {}", A1);
    logger.warn(BLUE, "m2 {} {}", A1, A2);
    logger.warn(BLUE, "m3 {} {} {}", A1, A2, A3);
    logger.warn(BLUE, "mt", EX);
    logger.warn(MARKERS, "m0");
    logger.warn(MARKERS, "m1 {}", A1);
    logger.warn(MARKERS, "m2 {} {}", A1, A2);
    logger.warn(MARKERS, "m3 {} {} {}", A1, A2, A3);
    logger.warn(MARKERS, "mt", EX);

    assertOverloadEvents(Level.WARN);
  }

  @Test
  public void errorOverloadsAppendEventsWithTheirMarkersArgumentsAndThrowable() {
    logger.setLevel(Level.ERROR);

    logger.error("m0");
    logger.error("m1 {}", A1);
    logger.error("m2 {} {}", A1, A2);
    logger.error("m3 {} {} {}", A1, A2, A3);
    logger.error("mt", EX);
    logger.error(BLUE, "m0");
    logger.error(BLUE, "m1 {}", A1);
    logger.error(BLUE, "m2 {} {}", A1, A2);
    logger.error(BLUE, "m3 {} {} {}", A1, A2, A3);
    logger.error(BLUE, "mt", EX);
    logger.error(MARKERS, "m0");
    logger.error(MARKERS, "m1 {}", A1);
    logger.error(MARKERS, "m2 {} {}", A1, A2);
    logger.error(MARKERS, "m3 {} {} {}", A1, A2, A3);
    logger.error(MARKERS, "mt", EX);

    assertOverloadEvents(Level.ERROR);
  }

  // ------------------------------------------- turbo filters on print calls

  @Test
  public void neutralReplyDropsOneAndTwoArgumentCallsBelowTheEffectiveLevel() {
    RecordingTurboFilter filter = addTurboFilter(FilterReply.NEUTRAL);
    logger.setLevel(Level.INFO);

    logger.debug("d1 {}", A1);
    logger.debug("d2 {} {}", A1, A2);
    assertEquals(0, appender.list.size());
    assertEquals(2, filter.calls);

    logger.info("i1 {}", A1);
    logger.info("i2 {} {}", A1, A2);
    assertEquals(2, appender.list.size());
    assertEquals("i1 {}", appender.list.get(0).getMessage());
    assertEquals("i2 {} {}", appender.list.get(1).getMessage());
  }

  @Test
  public void acceptReplyAppendsCallsBelowTheEffectiveLevel() {
    RecordingTurboFilter filter = addTurboFilter(FilterReply.ACCEPT);
    logger.setLevel(Level.OFF);

    logger.debug("d0");
    filter.assertAsked(Level.DEBUG, null, "d0", null);
    logger.debug("d1 {}", A1);
    filter.assertAsked(Level.DEBUG, null, "d1 {}", new Object[] { A1 });
    logger.debug(MARKERS, "d2 {} {}", A1, A2);
    filter.assertAsked(Level.DEBUG, MARKERS, "d2 {} {}", new Object[] { A1, A2 });

    assertEquals(3, appender.list.size());
    assertEquals("d0", appender.list.get(0).getFormattedMessage());
    assertEquals("d1 a1", appender.list.get(1).getFormattedMessage());
    assertEquals("d2 a1 a2", appender.list.get(2).getFormattedMessage());
    assertEquals(Level.DEBUG, appender.list.get(2).getLevel());
    assertSame(MARKERS, appender.list.get(2).getMarkers());
  }

  @Test
  public void denyReplyDropsCallsAboveTheEffectiveLevel() {
    RecordingTurboFilter filter = addTurboFilter(FilterReply.DENY);
    logger.setLevel(Level.ALL);

    logger.error("e0");
    logger.error("e1 {}", A1);
    logger.error("e2 {} {}", A1, A2);
    logger.error("e3 {} {} {}", A1, A2, A3);

    assertEquals(4, filter.calls);
    assertEquals(0, appender.list.size());
  }

  // ------------------------------------------------------- isXxxEnabled

  @Test
  public void markerEnabledChecksAskTurboFiltersAboutThatMarkerAtTheirLevel() {
    RecordingTurboFilter filter = addTurboFilter(FilterReply.NEUTRAL);
    logger.setLevel(Level.INFO);
    List<Marker> blueOnly = Collections.singletonList(BLUE);

    assertFalse(logger.isTraceEnabled(BLUE));
    filter.assertAsked(Level.TRACE, blueOnly, null, null);
    assertFalse(logger.isDebugEnabled(BLUE));
    filter.assertAsked(Level.DEBUG, blueOnly, null, null);
    assertTrue(logger.isInfoEnabled(BLUE));
    filter.assertAsked(Level.INFO, blueOnly, null, null);
    assertTrue(logger.isWarnEnabled(BLUE));
    filter.assertAsked(Level.WARN, blueOnly, null, null);
    assertTrue(logger.isErrorEnabled(BLUE));
    filter.assertAsked(Level.ERROR, blueOnly, null, null);
  }

  @Test
  public void acceptReplyEnablesEveryLevelOfALoggerThatIsOff() {
    addTurboFilter(FilterReply.ACCEPT);
    logger.setLevel(Level.OFF);

    assertTrue(logger.isTraceEnabled());
    assertTrue(logger.isDebugEnabled());
    assertTrue(logger.isInfoEnabled());
    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isErrorEnabled());
    assertTrue(logger.isEnabledFor(Level.TRACE));
  }

  @Test
  public void denyReplyDisablesEveryLevelOfALoggerThatAcceptsAll() {
    addTurboFilter(FilterReply.DENY);
    logger.setLevel(Level.ALL);

    assertFalse(logger.isTraceEnabled());
    assertFalse(logger.isDebugEnabled());
    assertFalse(logger.isInfoEnabled());
    assertFalse(logger.isWarnEnabled());
    assertFalse(logger.isErrorEnabled());
    assertFalse(logger.isEnabledFor(Level.ERROR));
  }

  @Test
  public void enabledChecksRejectAReplyOtherThanAcceptNeutralOrDeny() {
    addTurboFilter(null);

    assertUnknownReply(() -> logger.isTraceEnabled());
    assertUnknownReply(() -> logger.isDebugEnabled());
    assertUnknownReply(() -> logger.isInfoEnabled());
    assertUnknownReply(() -> logger.isWarnEnabled());
    assertUnknownReply(() -> logger.isErrorEnabled());
    assertUnknownReply(() -> logger.isEnabledFor(Level.INFO));
  }

  // -------------------------------------------------------------- log(...)

  @Test
  public void logWithMarkerListAppendsAtTheGivenLevelAndReportsTheCallerOfTheGivenClass() {
    logger.setLevel(Level.INFO);

    Facade.log(logger, MARKERS, LocationAwareLogger.DEBUG_INT, "dropped");
    assertEquals(0, appender.list.size());

    Facade.log(logger, MARKERS, LocationAwareLogger.WARN_INT, "w {}", A1);

    assertEquals(1, appender.list.size());
    ILoggingEvent e = appender.list.get(0);
    assertEquals(Level.WARN, e.getLevel());
    assertSame(MARKERS, e.getMarkers());
    assertEquals("w {}", e.getMessage());
    assertArrayEquals(new Object[] { A1 }, e.getArgumentArray());
    assertSame(EX, throwableOf(e));
    // the frames of the class named as fqcn are skipped: the caller is this test
    StackTraceElement caller = e.getCallerData()[0];
    assertEquals(LoggerApiTest.class.getName(), caller.getClassName());
    assertEquals("logWithMarkerListAppendsAtTheGivenLevelAndReportsTheCallerOfTheGivenClass",
        caller.getMethodName());
  }

  @Test
  public void logSlf4jEventReplaysItsLevelMarkersMessageArgumentsAndThrowable() {
    logger.setLevel(Level.INFO);

    DefaultLoggingEvent dropped = new DefaultLoggingEvent(org.slf4j.event.Level.DEBUG, logger);
    dropped.setMessage("dropped");
    logger.log(dropped);
    assertEquals(0, appender.list.size());

    DefaultLoggingEvent replayed = new DefaultLoggingEvent(org.slf4j.event.Level.WARN, logger);
    replayed.addMarker(BLUE);
    replayed.setMessage("w {} {}");
    replayed.addArguments(A1, A2);
    replayed.setThrowable(EX);
    logger.log(replayed);

    assertEquals(1, appender.list.size());
    ILoggingEvent e = appender.list.get(0);
    assertEquals(Level.WARN, e.getLevel());
    assertEquals(Collections.singletonList(BLUE), e.getMarkers());
    assertEquals("w {} {}", e.getMessage());
    assertArrayEquals(new Object[] { A1, A2 }, e.getArgumentArray());
    assertSame(EX, throwableOf(e));
    StackTraceElement caller = e.getCallerData()[0];
    assertEquals(LoggerApiTest.class.getName(), caller.getClassName());
    assertEquals("logSlf4jEventReplaysItsLevelMarkersMessageArgumentsAndThrowable",
        caller.getMethodName());
  }

  // ---------------------------------------------------------------- helpers

  /**
   * Checks the 15 events appended by the calls of a {@code *Overloads} test:
   * the five argument shapes without markers, with the {@code BLUE} marker and
   * with the {@code MARKERS} list.
   */
  private void assertOverloadEvents(Level level) {
    List<ILoggingEvent> events = appender.list;
    assertEquals(15, events.size());
    for (int i = 0; i < events.size(); i++) {
      ILoggingEvent e = events.get(i);
      int shape = i % 5;
      String call = level + " call #" + i;
      assertEquals(call, level, e.getLevel());
      assertEquals(call, logger.getName(), e.getLoggerName());
      assertEquals(call, SHAPE_MESSAGES[shape], e.getMessage());
      assertArrayEquals(call, SHAPE_ARGS[shape], e.getArgumentArray());
      assertSame(call, shape == THROWABLE_SHAPE ? EX : null, throwableOf(e));
      if (i < 5) {
        assertNull(call, e.getMarkers());
      } else if (i < 10) {
        assertEquals(call, Collections.singletonList(BLUE), e.getMarkers());
      } else {
        assertSame(call, MARKERS, e.getMarkers());
      }
    }
  }

  private static Throwable throwableOf(ILoggingEvent e) {
    IThrowableProxy proxy = e.getThrowableProxy();
    return proxy == null ? null : ((ThrowableProxy) proxy).getThrowable();
  }

  private static void assertUnknownReply(ThrowingRunnable check) {
    IllegalStateException e = assertThrows(IllegalStateException.class, check);
    assertEquals("Unknown FilterReply value: null", e.getMessage());
  }

  private RecordingTurboFilter addTurboFilter(FilterReply reply) {
    RecordingTurboFilter filter = new RecordingTurboFilter(reply);
    filter.setContext(lc);
    filter.start();
    lc.addTurboFilter(filter);
    return filter;
  }

  /** Replies with a fixed {@link FilterReply} and records what it was asked. */
  static final class RecordingTurboFilter extends TurboFilter {
    final FilterReply reply;
    int calls;
    List<Marker> markers;
    Level level;
    String format;
    Object[] params;

    RecordingTurboFilter(FilterReply reply) {
      this.reply = reply;
    }

    @Override
    public FilterReply decide(List<Marker> markers, Logger logger, Level level,
        String format, Object[] params, Throwable t) {
      calls++;
      this.markers = markers;
      this.level = level;
      this.format = format;
      this.params = params;
      return reply;
    }

    void assertAsked(Level expectedLevel, List<Marker> expectedMarkers,
        String expectedFormat, Object[] expectedParams) {
      assertEquals(expectedLevel, level);
      assertEquals(expectedMarkers, markers);
      assertEquals(expectedFormat, format);
      assertArrayEquals(expectedParams, params);
    }
  }

  /** Records events, computing their caller data while the call is on the stack. */
  static final class CallerDataCapturingAppender extends ListAppender<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent e) {
      e.getCallerData();
      super.append(e);
    }
  }

  /** A logging facade that passes its own class name as the caller boundary. */
  static final class Facade {
    static void log(Logger logger, List<Marker> markers, int levelInt, String msg, Object... args) {
      logger.log(markers, Facade.class.getName(), levelInt, msg, args, EX);
    }
  }
}
