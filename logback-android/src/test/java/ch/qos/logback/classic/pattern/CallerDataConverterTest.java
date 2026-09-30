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
package ch.qos.logback.classic.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

public class CallerDataConverterTest {

  private static final StackTraceElement[] CALLER_DATA = {
      new StackTraceElement("a.A", "m0", "A.java", 10),
      new StackTraceElement("b.B", "m1", "B.java", 20),
      new StackTraceElement("c.C", "m2", "C.java", 30),
      new StackTraceElement("d.D", "m3", "D.java", 40),
      new StackTraceElement("e.E", "m4", "E.java", 50),
      new StackTraceElement("f.F", "m5", "F.java", 60),
      new StackTraceElement("g.G", "m6", "G.java", 70),
  };

  private final LoggerContext lc = new LoggerContext();
  private final Logger logger = lc.getLogger(CallerDataConverterTest.class);

  private CallerDataConverter startedConverter(String... options) {
    return start(new CallerDataConverter(), options);
  }

  private CallerDataConverter start(CallerDataConverter converter, String... options) {
    converter.setContext(lc);
    converter.setOptionList(Arrays.asList(options));
    converter.start();
    return converter;
  }

  private LoggingEvent event() {
    LoggingEvent le = new LoggingEvent(getClass().getName(), logger, Level.INFO, "msg", null, null);
    le.setCallerData(CALLER_DATA);
    return le;
  }

  private static String callerLines(int from, int to) {
    StringBuilder sb = new StringBuilder();
    for (int i = from; i < to; i++) {
      sb.append("Caller+").append(i).append("\t at ").append(CALLER_DATA[i])
          .append(CoreConstants.LINE_SEPARATOR);
    }
    return sb.toString();
  }

  private List<Status> statuses() {
    return lc.getStatusManager().getCopyOfStatusList();
  }

  private void assertSingleError(String message, Class<? extends Throwable> throwableClass) {
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals(message, status.getMessage());
    if (throwableClass == null) {
      assertNull(status.getThrowable());
    } else {
      assertTrue(throwableClass.isInstance(status.getThrowable()));
    }
  }

  @Test
  public void withoutOptionsPrintsTopFiveFrames() {
    CallerDataConverter converter = startedConverter();
    assertEquals(callerLines(0, 5), converter.convert(event()));
  }

  @Test
  public void depthBeyondCallerDataPrintsAllFrames() {
    CallerDataConverter converter = startedConverter("9");
    assertEquals(callerLines(0, 7), converter.convert(event()));
  }

  @Test
  public void depthOptionLimitsNumberOfPrintedFrames() {
    CallerDataConverter converter = startedConverter("2");
    assertEquals(callerLines(0, 2), converter.convert(event()));
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void rangeOptionPrintsFramesFromStartUpToEnd() {
    CallerDataConverter converter = startedConverter("1..2");
    assertEquals(callerLines(1, 2), converter.convert(event()));
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void rangeMayStartAtTopFrame() {
    CallerDataConverter converter = startedConverter("0..1");
    assertEquals(callerLines(0, 1), converter.convert(event()));
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void rangeStartingBeyondCallerDataPrintsNotAvailable() {
    CallerDataConverter converter = startedConverter("7..9");
    assertEquals(CallerData.CALLER_DATA_NA, converter.convert(event()));
  }

  @Test
  public void missingCallerDataPrintsNotAvailable() {
    CallerDataConverter converter = startedConverter();
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getCallerData()).thenReturn(null);
    assertEquals(CallerData.CALLER_DATA_NA, converter.convert(event));
  }

  @Test
  public void unparsableDepthIsReportedAndDefaultDepthKept() {
    CallerDataConverter converter = startedConverter("abc");
    assertSingleError("Failed to parse depth option [abc]", NumberFormatException.class);
    assertEquals(callerLines(0, 5), converter.convert(event()));
  }

  @Test
  public void unparsableRangeBoundIsReported() {
    startedConverter("1..x");
    assertSingleError("Failed to parse depth option [1..x]", NumberFormatException.class);
  }

  @Test
  public void negativeRangeStartIsReported() {
    startedConverter("-1..3");
    assertSingleError(
        "Invalid depthStart/depthEnd range [-1, 3] (negative values are not allowed)", null);
  }

  @Test
  public void negativeRangeEndIsReported() {
    startedConverter("1..-3");
    assertSingleError(
        "Invalid depthStart/depthEnd range [1, -3] (negative values are not allowed)", null);
  }

  @Test
  public void rangeWithStartNotBelowEndIsReported() {
    startedConverter("2..2");
    assertSingleError("Invalid depthEnd range [2, 2] (start greater or equal to end)", null);
  }

  @Test
  public void rangeThatDoesNotSplitIntoTwoBoundsIsReported() {
    // With an empty delimiter every depth string "is" a range, but splitting the
    // empty string yields a single part.
    CallerDataConverter converter = new CallerDataConverter() {
      @Override
      protected String getDefaultRangeDelimiter() {
        return "";
      }
    };
    start(converter, "");
    assertSingleError("Failed to parse depth option as range []", null);
    assertEquals(callerLines(0, 5), converter.convert(event()));
  }

  @Test
  public void depthSuppliedWithoutOptionListRegistersNoEvaluator() {
    CallerDataConverter converter = new CallerDataConverter() {
      @Override
      public String getFirstOption() {
        return "1";
      }
    };
    converter.setContext(lc);
    converter.start();

    assertNull(converter.evaluatorList);
    assertEquals(callerLines(0, 1), converter.convert(event()));
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void evaluatorsNamedInOptionsAreLookedUpInContext() {
    StubEventEvaluator e1 = new StubEventEvaluator("e1", false).registerIn(lc);
    StubEventEvaluator e2 = new StubEventEvaluator("e2", false).registerIn(lc);

    CallerDataConverter converter = startedConverter("2", "e1", "unknown", "e2");

    assertEquals(2, converter.evaluatorList.size());
    assertSame(e1, converter.evaluatorList.get(0));
    assertSame(e2, converter.evaluatorList.get(1));
  }

  @Test
  public void callerDataPrintedOnceAnEvaluatorMatches() {
    StubEventEvaluator noMatch = new StubEventEvaluator("noMatch", false).registerIn(lc);
    StubEventEvaluator match = new StubEventEvaluator("match", true).registerIn(lc);
    StubEventEvaluator notReached = new StubEventEvaluator("notReached", true).registerIn(lc);
    CallerDataConverter converter = startedConverter("2", "noMatch", "match", "notReached");

    assertEquals(callerLines(0, 2), converter.convert(event()));
    assertEquals(1, noMatch.invocations);
    assertEquals(1, match.invocations);
    assertEquals("evaluation stops at the first match", 0, notReached.invocations);
  }

  @Test
  public void callerDataOmittedWhenNoEvaluatorMatches() {
    StubEventEvaluator e1 = new StubEventEvaluator("e1", false).registerIn(lc);
    StubEventEvaluator e2 = new StubEventEvaluator("e2", false).registerIn(lc);
    CallerDataConverter converter = startedConverter("2", "e1", "e2");

    assertEquals(CoreConstants.EMPTY_STRING, converter.convert(event()));
    assertEquals(1, e1.invocations);
    assertEquals(1, e2.invocations);
  }

  @Test
  public void evaluatorFailuresAreReportedUntilTheErrorLimit() {
    EvaluationException failure = new EvaluationException("boom");
    StubEventEvaluator failing = new StubEventEvaluator("failing", failure).registerIn(lc);
    CallerDataConverter converter = startedConverter("2", "failing");
    assertEquals(4, converter.MAX_ERROR_COUNT);

    for (int i = 0; i < 5; i++) {
      // a failing evaluator does not count as a match
      assertEquals(CoreConstants.EMPTY_STRING, converter.convert(event()));
    }

    assertEquals(5, converter.errorCount);
    List<Status> statuses = statuses();
    assertEquals(statuses.toString(), 4, statuses.size());
    for (int i = 0; i < 3; i++) {
      Status status = statuses.get(i);
      assertEquals(Status.ERROR, status.getLevel());
      assertEquals("Exception thrown for evaluator named [failing]", status.getMessage());
      assertSame(failure, status.getThrowable());
      assertSame(converter, status.getOrigin());
    }
    Status last = statuses.get(3);
    assertTrue(last instanceof ErrorStatus);
    assertEquals("Exception thrown for evaluator named [failing].", last.getMessage());
    assertSame(failure, last.getThrowable());
    assertSame(converter, last.getOrigin());
    assertTrue(last.hasChildren());
    Status child = last.iterator().next();
    assertEquals(Status.ERROR, child.getLevel());
    assertEquals("This was the last warning about this evaluator's errors. "
        + "We don't want the StatusManager to get flooded.", child.getMessage());
    assertSame(converter, child.getOrigin());
  }
}
