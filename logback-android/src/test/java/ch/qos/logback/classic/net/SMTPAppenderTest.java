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
package ch.qos.logback.classic.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.boolex.OnErrorEvaluator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import ch.qos.logback.core.helpers.CyclicBuffer;

public class SMTPAppenderTest {

  private final LoggerContext context = new LoggerContext();

  private LoggingEvent event(Level level, String message) {
    return new LoggingEvent(getClass().getName(), context.getLogger("test"), level, message, null, null);
  }

  private PatternLayout layout(String pattern) {
    PatternLayout layout = new PatternLayout();
    layout.setContext(context);
    layout.setPattern(pattern);
    layout.start();
    return layout;
  }

  private <A extends SMTPAppender> A configure(A appender) {
    appender.setContext(context);
    appender.setName("smtp");
    appender.setLayout(layout("%msg;"));
    appender.addTo("to@example.com");
    appender.setAsynchronousSending(false);
    return appender;
  }

  private static LoggingEvent withMarkers(LoggingEvent event, Marker... markers) {
    event.setMarkers(Arrays.asList(markers));
    return event;
  }

  // ------------------------------------------------------------- evaluator

  @Test
  public void defaultEvaluatorSendsOnErrorEvents() {
    BufferRecordingSMTPAppender appender = configure(new BufferRecordingSMTPAppender());
    appender.start();

    EventEvaluator<ILoggingEvent> evaluator = appender.evaluator();
    assertTrue(evaluator instanceof OnErrorEvaluator);
    assertEquals("onError", evaluator.getName());
    assertSame(context, evaluator.getContext());
    assertTrue(evaluator.isStarted());

    appender.doAppend(event(Level.WARN, "warn"));
    assertTrue(appender.sentBuffers.isEmpty());

    appender.doAppend(event(Level.ERROR, "error"));
    assertEquals(Collections.singletonList(Arrays.asList("warn", "error")), appender.sentBuffers);
  }

  @Test
  public void evaluatorGivenToTheConstructorIsKeptAtStart() {
    EventEvaluatorBase<ILoggingEvent> onSendMessage = new EventEvaluatorBase<ILoggingEvent>() {
      @Override
      public boolean evaluate(ILoggingEvent event) throws EvaluationException {
        return "send".equals(event.getMessage());
      }
    };
    BufferRecordingSMTPAppender appender = configure(new BufferRecordingSMTPAppender(onSendMessage));
    appender.start();

    // the default evaluator would have sent on this ERROR event
    appender.doAppend(event(Level.ERROR, "error"));
    assertTrue(appender.sentBuffers.isEmpty());

    appender.doAppend(event(Level.DEBUG, "send"));
    assertEquals(Collections.singletonList(Arrays.asList("error", "send")), appender.sentBuffers);
  }

  // ------------------------------------------------------------- subAppend

  @Test
  public void callerDataIsNotIncludedByDefault() {
    SMTPAppender appender = new SMTPAppender();
    assertFalse(appender.isIncludeCallerData());

    LoggingEvent event = event(Level.ERROR, "x");
    CyclicBuffer<ILoggingEvent> cb = new CyclicBuffer<ILoggingEvent>(4);
    appender.subAppend(cb, event);

    assertFalse(event.hasCallerData());
    assertEquals(1, cb.length());
  }

  @Test
  public void callerDataIsExtractedBeforeBufferingWhenIncluded() {
    SMTPAppender appender = new SMTPAppender();
    appender.setIncludeCallerData(true);
    assertTrue(appender.isIncludeCallerData());

    LoggingEvent event = event(Level.ERROR, "x");
    CyclicBuffer<ILoggingEvent> cb = new CyclicBuffer<ILoggingEvent>(4);
    appender.subAppend(cb, event);

    assertTrue(event.hasCallerData());
    assertNotNull(event.getCallerData());
    assertEquals(Collections.<ILoggingEvent>singletonList(event), cb.asList());
  }

  @Test
  public void bufferedEventKeepsItsMessageAsFormattedWhenItWasAppended() {
    SMTPAppender appender = new SMTPAppender();
    StringBuilder state = new StringBuilder("before");
    LoggingEvent event = new LoggingEvent(getClass().getName(), context.getLogger("test"), Level.ERROR,
        "state={}", null, new Object[] {state});
    CyclicBuffer<ILoggingEvent> cb = new CyclicBuffer<ILoggingEvent>(4);

    appender.subAppend(cb, event);
    // the argument changes before the buffered event is laid out in an e-mail
    state.replace(0, state.length(), "after");

    assertEquals("state=before", cb.get().getFormattedMessage());
  }

  // ------------------------------------------------------------- subject

  @Test
  public void defaultSubjectIsTheLoggerAndMessageWithoutTheException() {
    SMTPAppender appender = new SMTPAppender();
    appender.setContext(context);
    Layout<ILoggingEvent> subject = appender.makeSubjectLayout(null);

    LoggingEvent event = new LoggingEvent(getClass().getName(), context.getLogger("test"), Level.ERROR,
        "failed", new IllegalStateException("boom"), null);

    assertTrue(subject.isStarted());
    assertEquals("test - failed", subject.doLayout(event));
  }

  @Test
  public void configuredSubjectPatternIsUsedWithoutTheException() {
    SMTPAppender appender = new SMTPAppender();
    appender.setContext(context);
    Layout<ILoggingEvent> subject = appender.makeSubjectLayout("[%level] %msg");

    LoggingEvent event = new LoggingEvent(getClass().getName(), context.getLogger("test"), Level.ERROR,
        "failed", new IllegalStateException("boom"), null);

    assertEquals("[ERROR] failed", subject.doLayout(event));
  }

  // ------------------------------------------------------------- fillBuffer

  @Test
  public void fillBufferLaysOutEveryBufferedEventInOrderAndDrainsTheBuffer() {
    SMTPAppender appender = new SMTPAppender();
    appender.setLayout(layout("<%msg>"));
    CyclicBuffer<ILoggingEvent> cb = new CyclicBuffer<ILoggingEvent>(4);
    cb.add(event(Level.INFO, "one"));
    cb.add(event(Level.ERROR, "two"));
    StringBuffer sbuf = new StringBuffer("start:");

    appender.fillBuffer(cb, sbuf);

    assertEquals("start:<one><two>", sbuf.toString());
    assertEquals(0, cb.length());
  }

  // ----------------------------------------------------- eventMarksEndOfLife

  @Test
  public void eventWithoutMarkersDoesNotMarkEndOfLife() {
    assertFalse(new SMTPAppender().eventMarksEndOfLife(event(Level.INFO, "x")));
  }

  @Test
  public void eventWithEmptyMarkerListDoesNotMarkEndOfLife() {
    LoggingEvent event = event(Level.INFO, "x");
    event.setMarkers(Collections.<Marker>emptyList());
    assertFalse(new SMTPAppender().eventMarksEndOfLife(event));
  }

  @Test
  public void eventWithUnrelatedMarkersDoesNotMarkEndOfLife() {
    LoggingEvent event = withMarkers(event(Level.INFO, "x"),
        MarkerFactory.getDetachedMarker("A"), MarkerFactory.getDetachedMarker("B"));
    assertFalse(new SMTPAppender().eventMarksEndOfLife(event));
  }

  @Test
  public void finalizeSessionMarkerMarksEndOfLife() {
    LoggingEvent event = withMarkers(event(Level.INFO, "x"), ClassicConstants.FINALIZE_SESSION_MARKER);
    assertTrue(new SMTPAppender().eventMarksEndOfLife(event));
  }

  @Test
  public void markerReferencingFinalizeSessionAfterAnUnrelatedOneMarksEndOfLife() {
    Marker parent = MarkerFactory.getDetachedMarker("PARENT");
    parent.add(ClassicConstants.FINALIZE_SESSION_MARKER);
    LoggingEvent event = withMarkers(event(Level.INFO, "x"), MarkerFactory.getDetachedMarker("UNRELATED"), parent);
    assertTrue(new SMTPAppender().eventMarksEndOfLife(event));
  }

  /**
   * Records the messages of the buffered events it would send instead of
   * sending an e-mail.
   */
  static class BufferRecordingSMTPAppender extends SMTPAppender {

    final List<List<String>> sentBuffers = new ArrayList<List<String>>();

    BufferRecordingSMTPAppender() {
    }

    BufferRecordingSMTPAppender(EventEvaluatorBase<ILoggingEvent> evaluator) {
      super(evaluator);
    }

    EventEvaluator<ILoggingEvent> evaluator() {
      return eventEvaluator;
    }

    @Override
    protected void sendBuffer(CyclicBuffer<ILoggingEvent> cb, ILoggingEvent lastEventObject) {
      List<String> messages = new ArrayList<String>();
      for (ILoggingEvent event : cb.asList()) {
        messages.add(event.getMessage());
      }
      sentBuffers.add(messages);
    }
  }
}
