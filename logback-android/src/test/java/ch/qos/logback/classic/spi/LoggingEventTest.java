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
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.spi.MDCAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class LoggingEventTest {

  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

  @Before
  public void setUp() {
  }


  // Issue #365: getMarkerList is logback-classic's name for getMarkers
  @Test
  public void markerListIsSynonymForMarkers() {
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);
    org.slf4j.Marker marker = org.slf4j.MarkerFactory.getMarker("EVENT_365");
    event.setMarkers(java.util.Collections.singletonList(marker));

    assertEquals(event.getMarkers(), event.getMarkerList());
    assertEquals(1, event.getMarkerList().size());
    assertEquals(marker, event.getMarkerList().get(0));
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

  @Test
  public void trailingThrowableArgumentBecomesTheEventThrowable() {
    Exception ex = new Exception("boom");
    Object[] argArray = new Object[] {12, ex};

    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "x={}", null, argArray);

    assertArrayEquals(new Object[] {12}, event.getArgumentArray());
    assertEquals(2, argArray.length);
    assertSame(ex, ((ThrowableProxy) event.getThrowableProxy()).getThrowable());
    assertEquals("x=12", event.getFormattedMessage());
  }

  @Test
  public void explicitThrowableLeavesTheArgumentsUntouched() {
    Exception explicit = new Exception("explicit");
    Exception trailing = new Exception("trailing");
    Object[] argArray = new Object[] {12, trailing};

    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "x={} {}", explicit, argArray);

    assertSame(argArray, event.getArgumentArray());
    assertSame(explicit, ((ThrowableProxy) event.getThrowableProxy()).getThrowable());
  }

  @Test
  public void packagingDataIsCalculatedWhenEnabledInTheContext() {
    loggerContext.setPackagingDataEnabled(true);

    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", new Exception("boom"), null);

    StackTraceElementProxy[] steps = event.getThrowableProxy().getStackTraceElementProxyArray();
    assertTrue(steps.length > 0);
    for (StackTraceElementProxy step : steps) {
      assertNotNull(step.getClassPackagingData());
    }
  }

  @Test
  public void packagingDataIsNotCalculatedByDefault() {
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", new Exception("boom"), null);

    StackTraceElementProxy[] steps = event.getThrowableProxy().getStackTraceElementProxyArray();
    assertTrue(steps.length > 0);
    for (StackTraceElementProxy step : steps) {
      assertNull(step.getClassPackagingData());
    }
  }

  @Test
  public void argumentArrayCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    Object[] first = new Object[] {1};
    event.setArgumentArray(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> event.setArgumentArray(new Object[] {2}));
    assertEquals("argArray has been already set", e.getMessage());
    assertSame(first, event.getArgumentArray());
  }

  @Test
  public void threadNameCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    event.setThreadName("first");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> event.setThreadName("second"));
    assertEquals("threadName has been already set", e.getMessage());
    assertEquals("first", event.getThreadName());
  }

  @Test
  public void threadNameCannotBeSetAfterItWasComputed() {
    LoggingEvent event = new LoggingEvent();
    assertEquals(Thread.currentThread().getName(), event.getThreadName());

    assertThrows(IllegalStateException.class, () -> event.setThreadName("other"));
  }

  @Test
  public void throwableProxyCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    ThrowableProxy first = new ThrowableProxy(new Exception("first"));
    event.setThrowableProxy(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> event.setThrowableProxy(new ThrowableProxy(new Exception("second"))));
    assertEquals("ThrowableProxy has been already set.", e.getMessage());
    assertSame(first, event.getThrowableProxy());
  }

  @Test
  public void messageCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    event.setMessage("first");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> event.setMessage("second"));
    assertEquals("The message for this event has been set already.", e.getMessage());
    assertEquals("first", event.getMessage());
  }

  @Test
  public void levelCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    event.setLevel(Level.WARN);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> event.setLevel(Level.ERROR));
    assertEquals("The level has been already set for this event.", e.getMessage());
    assertEquals(Level.WARN, event.getLevel());
  }

  @Test
  public void markersCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    List<Marker> first = Collections.singletonList(MarkerFactory.getMarker("FIRST"));
    event.setMarkers(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> event.setMarkers(Collections.singletonList(MarkerFactory.getMarker("SECOND"))));
    assertEquals("The marker has been already set for this event.", e.getMessage());
    assertSame(first, event.getMarkers());
  }

  @Test
  public void mdcPropertyMapCanBeSetOnlyOnce() {
    LoggingEvent event = new LoggingEvent();
    Map<String, String> first = Collections.singletonMap("k", "first");
    event.setMDCPropertyMap(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> event.setMDCPropertyMap(Collections.singletonMap("k", "second")));
    assertEquals("The MDCPropertyMap has been already set for this event.", e.getMessage());
    assertSame(first, event.getMDCPropertyMap());
  }

  @Test
  public void mdcIsASynonymForMdcPropertyMap() {
    LoggingEvent event = new LoggingEvent();
    Map<String, String> map = Collections.singletonMap("k", "v");
    event.setMDCPropertyMap(map);

    assertSame(map, event.getMdc());
  }

  @Test
  public void mdcPropertyMapIsCopiedFromAForeignMdcAdapter() {
    Map<String, String> context = new HashMap<String, String>();
    context.put("user", "alice");
    MDCAdapter foreignAdapter = mock(MDCAdapter.class);
    when(foreignAdapter.getCopyOfContextMap()).thenReturn(context);

    try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
      mdc.when(MDC::getMDCAdapter).thenReturn(foreignAdapter);
      LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);

      assertEquals(context, event.getMDCPropertyMap());
    }
  }

  @Test
  public void mdcPropertyMapIsEmptyWhenAForeignMdcAdapterHasNoContext() {
    MDCAdapter foreignAdapter = mock(MDCAdapter.class);
    when(foreignAdapter.getCopyOfContextMap()).thenReturn(null);

    try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
      mdc.when(MDC::getMDCAdapter).thenReturn(foreignAdapter);
      LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);

      assertEquals(Collections.<String, String>emptyMap(), event.getMDCPropertyMap());
    }
  }

  @Test
  public void contextBirthTimeComesFromTheContextView() {
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);

    assertEquals(loggerContext.getBirthTime(), event.getContextBirthTime());
  }

  @Test
  public void toStringShowsLevelAndFormattedMessage() {
    LoggingEvent event = new LoggingEvent("", logger, Level.WARN, "x={}", null, new Object[] {12});

    assertEquals("[WARN] x=12", event.toString());
  }

  @Test
  public void eventIsNotSerializable() throws IOException {
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);
    ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
    try {
      assertThrows(NotSerializableException.class, () -> oos.writeObject(event));
    } finally {
      oos.close();
    }
  }

  @Test
  public void serializationHookRefusesToWriteTheEvent() throws Exception {
    // LoggingEvent is not Serializable, so only reflection reaches its private
    // writeObject hook (kept from upstream logback)
    LoggingEvent event = new LoggingEvent("", logger, Level.INFO, "msg", null, null);
    Method writeObject = LoggingEvent.class.getDeclaredMethod("writeObject", ObjectOutputStream.class);
    writeObject.setAccessible(true);
    ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
    try {
      InvocationTargetException e = assertThrows(InvocationTargetException.class,
          () -> writeObject.invoke(event, oos));
      assertTrue(e.getCause() instanceof UnsupportedOperationException);
      assertEquals(LoggingEvent.class + " does not support serialization. "
          + "Use LoggerEventVO instance instead. See also LoggerEventVO.build method.", e.getCause().getMessage());
    } finally {
      oos.close();
    }
  }
}
