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
package ch.qos.logback.classic.sift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import ch.qos.logback.core.sift.AppenderFactory;
import ch.qos.logback.core.sift.AppenderTracker;

/**
 * Plain JVM tests of how {@link SiftingAppender} decides that an event ends the
 * life of its nested appender (see {@link ClassicConstants#FINALIZE_SESSION_MARKER}).
 * The Joran-based tests of SiftingAppender are in SiftingAppenderTest.
 */
public class SiftingAppenderEndOfLifeTest {

  IMarkerFactory markerFactory = new BasicMarkerFactory();
  Marker unrelated = markerFactory.getDetachedMarker("UNRELATED");
  Marker otherUnrelated = markerFactory.getDetachedMarker("OTHER_UNRELATED");

  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(this.getClass());
  SiftingAppender sa = new SiftingAppender();
  long now = 3000;

  @Before
  public void setUp() {
    MessageDiscriminator discriminator = new MessageDiscriminator();
    discriminator.start();
    sa.setContext(loggerContext);
    sa.setDiscriminator(discriminator);
    sa.setAppenderFactory(new ListAppenderFactory());
    sa.start();
    assertTrue(sa.isStarted());
  }

  @Test
  public void eventWithoutMarkersDoesNotEndLife() {
    assertFalse(sa.eventMarksEndOfLife(event("a", now)));
  }

  @Test
  public void eventWithOnlyUnrelatedMarkersDoesNotEndLife() {
    assertFalse(sa.eventMarksEndOfLife(event("a", now, unrelated, otherUnrelated)));
  }

  @Test
  public void finalizeSessionMarkerAfterUnrelatedMarkersEndsLife() {
    assertTrue(sa.eventMarksEndOfLife(
        event("a", now, unrelated, ClassicConstants.FINALIZE_SESSION_MARKER)));
  }

  @Test
  public void markerReferencingFinalizeSessionMarkerEndsLife() {
    Marker parent = markerFactory.getDetachedMarker("PARENT");
    parent.add(ClassicConstants.FINALIZE_SESSION_MARKER);

    assertTrue(sa.eventMarksEndOfLife(event("a", now, parent)));
  }

  @Test
  public void timestampOfTheEventIsUsed() {
    assertEquals(12345L, sa.getTimestamp(event("a", 12345L)));
  }

  @Test
  public void nestedAppenderOfAnEventWithUnrelatedMarkersOutlivesTheLingeringTimeout() {
    sa.doAppend(event("a", now, unrelated));
    Appender<ILoggingEvent> nested = sa.getAppenderTracker().find("a");

    // appending another event triggers the removal of stale nested appenders
    sa.doAppend(event("b", now + AppenderTracker.LINGERING_TIMEOUT + 1));

    assertSame(nested, sa.getAppenderTracker().find("a"));
    assertTrue(nested.isStarted());
    assertEquals(1, ((ListAppender<ILoggingEvent>) nested).list.size());
  }

  @Test
  public void nestedAppenderOfAFinalizingEventIsStoppedAfterTheLingeringTimeout() {
    sa.doAppend(event("a", now, unrelated, ClassicConstants.FINALIZE_SESSION_MARKER));
    Appender<ILoggingEvent> nested = sa.getAppenderTracker().find("a");

    sa.doAppend(event("b", now + AppenderTracker.LINGERING_TIMEOUT + 1));

    assertNull(sa.getAppenderTracker().find("a"));
    assertFalse(nested.isStarted());
    assertEquals(1, ((ListAppender<ILoggingEvent>) nested).list.size());
  }

  private LoggingEvent event(String message, long timestamp, Marker... markers) {
    LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, message, null, null);
    event.setTimeStamp(timestamp);
    if (markers.length > 0) {
      event.setMarkers(Arrays.asList(markers));
    }
    return event;
  }

  /** Sifts events by their message. */
  static class MessageDiscriminator extends AbstractDiscriminator<ILoggingEvent> {
    @Override
    public String getDiscriminatingValue(ILoggingEvent event) {
      return event.getMessage();
    }

    @Override
    public String getKey() {
      return "message";
    }
  }

  static class ListAppenderFactory implements AppenderFactory<ILoggingEvent> {
    @Override
    public Appender<ILoggingEvent> buildAppender(Context context, String discriminatingValue) {
      ListAppender<ILoggingEvent> la = new ListAppender<ILoggingEvent>();
      la.setContext(context);
      la.setName(discriminatingValue);
      la.start();
      return la;
    }
  }
}
