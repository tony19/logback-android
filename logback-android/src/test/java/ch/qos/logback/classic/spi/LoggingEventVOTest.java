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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;

public class LoggingEventVOTest {

  // larger than 2^32 so that the upper half of the time stamp takes part in hashCode()
  static final long TIME_STAMP = 0x1234_5678_9ABCL;

  static final Marker MARKER_A = MarkerFactory.getDetachedMarker("A");
  static final Marker MARKER_B = MarkerFactory.getDetachedMarker("B");

  /** A new String instance, equal to but not the same as {@code s}. */
  static String copyOf(String s) {
    return new StringBuilder(s).toString();
  }

  static List<Marker> markers(Marker... markers) {
    List<Marker> list = new ArrayList<Marker>();
    Collections.addAll(list, markers);
    return list;
  }

  static Map<String, String> mdc(String key, String value) {
    Map<String, String> map = new HashMap<String, String>();
    map.put(key, value);
    return map;
  }

  /**
   * A source event with every field that LoggingEventVO copies set. Each call
   * returns fresh (equal, but not identical) field values.
   */
  static PubLoggingEventVO fullSource() {
    PubLoggingEventVO source = new PubLoggingEventVO();
    source.message = copyOf("hello {}");
    source.loggerName = copyOf("a.b.C");
    source.threadName = copyOf("worker-1");
    source.timeStamp = TIME_STAMP;
    source.level = Level.INFO;
    source.argumentArray = new Object[] { "world" };
    source.markers = markers(MARKER_A);
    source.mdcPropertyMap = mdc("k", copyOf("v"));
    source.loggerContextVO = new LoggerContextVO("ctx", Collections.<String, String>emptyMap(), 77L);
    return source;
  }

  /** A full source event whose field {@code fieldName} is replaced by {@code value}. */
  static LoggingEventVO fullEventWith(String fieldName, Object value) throws Exception {
    PubLoggingEventVO source = fullSource();
    PubLoggingEventVO.class.getField(fieldName).set(source, value);
    return LoggingEventVO.build(source);
  }

  @Test
  public void buildCopiesTheStateOfTheSourceEvent() {
    PubLoggingEventVO source = fullSource();
    LoggingEventVO vo = LoggingEventVO.build(source);

    assertEquals("hello {}", vo.getMessage());
    assertEquals("a.b.C", vo.getLoggerName());
    assertEquals("worker-1", vo.getThreadName());
    assertEquals(TIME_STAMP, vo.getTimeStamp());
    assertSame(Level.INFO, vo.getLevel());
    assertArrayEquals(new Object[] { "world" }, vo.getArgumentArray());
    assertSame(source.markers, vo.getMarkers());
    assertSame(source.mdcPropertyMap, vo.getMDCPropertyMap());
    assertSame(source.loggerContextVO, vo.getLoggerContextVO());
    assertNull(vo.getThrowableProxy());
  }

  @Test
  public void getMdcReturnsTheMdcPropertyMap() {
    PubLoggingEventVO source = fullSource();
    LoggingEventVO vo = LoggingEventVO.build(source);

    assertSame(source.mdcPropertyMap, vo.getMdc());
    assertSame(vo.getMDCPropertyMap(), vo.getMdc());
  }

  @Test
  public void contextRemoteViewAndBirthTimeComeFromTheLoggerContextVO() {
    PubLoggingEventVO source = fullSource();
    LoggingEventVO vo = LoggingEventVO.build(source);

    assertSame(source.loggerContextVO, vo.getContextLoggerRemoteView());
    assertEquals(77L, vo.getContextBirthTime());
  }

  @Test
  public void callerDataIsCopiedWhenTheSourceHasIt() {
    PubLoggingEventVO source = fullSource();
    StackTraceElement[] callerData = { new StackTraceElement("a.b.C", "m", "C.java", 12) };
    source.callerDataArray = callerData;

    LoggingEventVO vo = LoggingEventVO.build(source);

    assertTrue(vo.hasCallerData());
    assertSame(callerData, vo.getCallerData());
  }

  @Test
  public void callerDataIsAbsentWhenTheSourceHasNone() {
    LoggingEventVO vo = LoggingEventVO.build(fullSource());

    assertFalse(vo.hasCallerData());
    assertNull(vo.getCallerData());
  }

  @Test
  public void formattedMessageSubstitutesArguments() {
    LoggingEventVO vo = LoggingEventVO.build(fullSource());
    assertEquals("hello world", vo.getFormattedMessage());
  }

  @Test
  public void formattedMessageIsTheRawMessageWithoutArguments() throws Exception {
    LoggingEventVO vo = fullEventWith("argumentArray", null);
    assertEquals("hello {}", vo.getFormattedMessage());
  }

  @Test
  public void formattedMessageIsComputedOnceAndCached() {
    PubLoggingEventVO source = fullSource();
    LoggingEventVO vo = LoggingEventVO.build(source);

    String first = vo.getFormattedMessage();
    // the VO shares the argument array of its source; changing it afterwards
    // must not change the already formatted message
    source.argumentArray[0] = "changed";

    assertEquals("hello world", first);
    assertSame(first, vo.getFormattedMessage());
  }

  @Test
  public void prepareForDeferredProcessingLeavesTheEventUnchanged() {
    PubLoggingEventVO source = fullSource();
    LoggingEventVO vo = LoggingEventVO.build(source);

    vo.prepareForDeferredProcessing();

    assertEquals(LoggingEventVO.build(fullSource()), vo);
    assertEquals("worker-1", vo.getThreadName());
    assertSame(source.mdcPropertyMap, vo.getMDCPropertyMap());
    assertFalse(vo.hasCallerData());
    assertEquals("hello world", vo.getFormattedMessage());
  }

  @Test
  public void equalsIsReflexive() {
    LoggingEventVO vo = LoggingEventVO.build(fullSource());
    assertTrue(vo.equals(vo));
  }

  @Test
  public void equalsRejectsNullAndOtherTypes() {
    LoggingEventVO vo = LoggingEventVO.build(fullSource());
    assertFalse(vo.equals(null));
    assertFalse(vo.equals("hello {}"));
  }

  @Test
  public void equalsRejectsSubclassInstancesWithTheSameState() {
    LoggingEventVO plain = LoggingEventVO.build(new PubLoggingEventVO());
    LoggingEventVO subclass = new LoggingEventVO() {
      private static final long serialVersionUID = 1L;
    };
    assertFalse(plain.equals(subclass));
    assertFalse(subclass.equals(plain));
  }

  @Test
  public void equalsHoldsForDistinctEventsWithEqualState() {
    LoggingEventVO a = LoggingEventVO.build(fullSource());
    LoggingEventVO b = LoggingEventVO.build(fullSource());
    assertNotSame(a.getMessage(), b.getMessage());

    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void equalsHoldsWhenAllComparedFieldsAreNullOnBothSides() {
    LoggingEventVO a = LoggingEventVO.build(new PubLoggingEventVO());
    LoggingEventVO b = LoggingEventVO.build(new PubLoggingEventVO());

    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void equalsComparesMessages() throws Exception {
    assertFieldTakesPartInEquals("message", copyOf("hello {}"), "bye");
  }

  @Test
  public void equalsComparesLoggerNames() throws Exception {
    assertFieldTakesPartInEquals("loggerName", copyOf("a.b.C"), "x.y.Z");
  }

  @Test
  public void equalsComparesThreadNames() throws Exception {
    assertFieldTakesPartInEquals("threadName", copyOf("worker-1"), "worker-2");
  }

  @Test
  public void equalsComparesMarkers() throws Exception {
    assertFieldTakesPartInEquals("markers", markers(MARKER_A), markers(MARKER_B));
  }

  @Test
  public void equalsComparesMdcPropertyMaps() throws Exception {
    assertFieldTakesPartInEquals("mdcPropertyMap", mdc("k", "v"), mdc("k", "w"));
  }

  @Test
  public void equalsComparesTimeStamps() throws Exception {
    LoggingEventVO base = LoggingEventVO.build(fullSource());
    LoggingEventVO later = fullEventWith("timeStamp", TIME_STAMP + 1);

    assertFalse(base.equals(later));
    assertFalse(later.equals(base));
  }

  @Test
  public void hashCodeCombinesMessageThreadNameAndTimeStamp() {
    LoggingEventVO vo = LoggingEventVO.build(fullSource());
    int expected = 31 * (31 * (31 + "hello {}".hashCode()) + "worker-1".hashCode())
        + (int) (TIME_STAMP ^ (TIME_STAMP >>> 32));
    assertEquals(expected, vo.hashCode());
  }

  @Test
  public void hashCodeTreatsNullMessageAndThreadNameAsZero() {
    PubLoggingEventVO source = new PubLoggingEventVO();
    source.timeStamp = TIME_STAMP;
    LoggingEventVO vo = LoggingEventVO.build(source);

    assertEquals(31 * 31 * 31 + (int) (TIME_STAMP ^ (TIME_STAMP >>> 32)), vo.hashCode());
  }

  @Test
  public void hashCodeChangesWithMessageThreadNameAndTimeStamp() throws Exception {
    int base = LoggingEventVO.build(fullSource()).hashCode();

    assertNotEquals(base, fullEventWith("message", "bye").hashCode());
    assertNotEquals(base, fullEventWith("threadName", "worker-2").hashCode());
    assertNotEquals(base, fullEventWith("timeStamp", TIME_STAMP + 1).hashCode());
  }

  /**
   * Checks every null/non-null combination of {@code fieldName} in equals():
   * an equal value (distinct instance) keeps the events equal, a different
   * value or a null on only one side makes them unequal, and null on both
   * sides keeps them equal.
   */
  private static void assertFieldTakesPartInEquals(String fieldName, Object equalValue, Object differentValue)
      throws Exception {
    LoggingEventVO base = LoggingEventVO.build(fullSource());
    LoggingEventVO same = fullEventWith(fieldName, equalValue);
    LoggingEventVO different = fullEventWith(fieldName, differentValue);
    LoggingEventVO nulled = fullEventWith(fieldName, null);
    LoggingEventVO alsoNulled = fullEventWith(fieldName, null);

    assertTrue(fieldName + ": equal values", base.equals(same));
    assertFalse(fieldName + ": different values", base.equals(different));
    assertFalse(fieldName + ": different values (reversed)", different.equals(base));
    assertFalse(fieldName + ": only other is null", base.equals(nulled));
    assertFalse(fieldName + ": only this is null", nulled.equals(base));
    assertTrue(fieldName + ": null on both sides", nulled.equals(alsoNulled));
  }
}
