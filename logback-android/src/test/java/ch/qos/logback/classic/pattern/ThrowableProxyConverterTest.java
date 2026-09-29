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

import static junit.framework.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.TestHelper;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

import static ch.qos.logback.classic.util.TestHelper.addSuppressed;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThrowableProxyConverterTest {

  LoggerContext lc = new LoggerContext();
  ThrowableProxyConverter tpc = new ThrowableProxyConverter();
  StringWriter sw = new StringWriter();
  PrintWriter pw = new PrintWriter(sw);

  @Before
  public void setUp() throws Exception {
    tpc.setContext(lc);
    tpc.start();
  }

  @After
  public void tearDown() throws Exception {
  }

  private ILoggingEvent createLoggingEvent(Throwable t) {
    return new LoggingEvent(this.getClass().getName(), lc
        .getLogger(Logger.ROOT_LOGGER_NAME), Level.DEBUG, "test message", t,
        null);
  }

  @Test
  public void suppressed() throws InvocationTargetException, IllegalAccessException
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(e, fooException);
      addSuppressed(e, barException);
      ex = e;
    }
    verify(ex);
  }

  @Test
  public void suppressedWithCause() throws InvocationTargetException, IllegalAccessException
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      ex=new Exception("Wrapper", e);
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(ex, fooException);
      addSuppressed(e, barException);
    }
    verify(ex);
  }

  @Test
  public void suppressedWithSuppressed() throws Exception
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      ex=new Exception("Wrapper", e);
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(barException, fooException);
      addSuppressed(e, barException);
    }
    verify(ex);
  }

  @Test
  public void smoke() {
    Exception t = new Exception("smoke");
    verify(t);
  }

  @Test
  public void nested() {
    Throwable t = TestHelper.makeNestedException(1);
    verify(t);
  }

  @Test
  public void withArgumentOfOne() throws Exception {
    final Throwable t = TestHelper.makeNestedException(0);
    t.printStackTrace(pw);
    final ILoggingEvent le = createLoggingEvent(t);

    final List<String> optionList = Arrays.asList("1");
    tpc.setOptionList(optionList);
    tpc.start();

    final String result = tpc.convert(le);

    final BufferedReader reader = new BufferedReader(new StringReader(result));
    assertTrue(reader.readLine().contains(t.getMessage()));
    assertNotNull(reader.readLine());
    assertNull("Unexpected line in stack trace", reader.readLine());
  }

  @Test
  public void withShortArgument() throws Exception {
    final Throwable t = TestHelper.makeNestedException(0);
    t.printStackTrace(pw);
    final ILoggingEvent le = createLoggingEvent(t);

    final List<String> options = Arrays.asList("short");
    tpc.setOptionList(options);
    tpc.start();

    final String result = tpc.convert(le);

    final BufferedReader reader = new BufferedReader(new StringReader(result));
    assertTrue(reader.readLine().contains(t.getMessage()));
    assertNotNull(reader.readLine());
    assertNull("Unexpected line in stack trace", reader.readLine());
  }

  @Test
  public void skipSelectedLine() throws Exception {
    String nameOfContainingMethod = "skipSelectedLine";
    //given
    final Throwable t = TestHelper.makeNestedException(0);
    t.printStackTrace(pw);
    final ILoggingEvent le = createLoggingEvent(t);
    tpc.setOptionList(Arrays.asList("full", nameOfContainingMethod));
    tpc.start();

    //when
    final String result = tpc.convert(le);

    //then
    assertThat(result, is(not(emptyString())));
    assertThat(result, not(containsString(nameOfContainingMethod)));
  }

  @Test
  public void shouldLimitTotalLinesExcludingSkipped() throws Exception {
    //given
    final Throwable t = TestHelper.makeNestedException(0);
    t.printStackTrace(pw);
    final ILoggingEvent le = createLoggingEvent(t);
    tpc.setOptionList(Arrays.asList("3", "shouldLimitTotalLinesExcludingSkipped"));
    tpc.start();

    //when
    final String result = tpc.convert(le);

    //then
    String[] lines = result.split(CoreConstants.LINE_SEPARATOR);
    assertThat(lines, Matchers.<String>arrayWithSize(3 + 1));
  }

  private static final String LS = CoreConstants.LINE_SEPARATOR;
  private static final StackTraceElement FRAME_A = new StackTraceElement("a.A", "m", "A.java", 1);
  private static final StackTraceElement FRAME_B = new StackTraceElement("b.B", "m", "B.java", 2);
  private static final StackTraceElement FRAME_C = new StackTraceElement("c.C", "m", "C.java", 3);

  /** Event carrying an exception whose stack trace is exactly FRAME_A, FRAME_B, FRAME_C. */
  private ILoggingEvent eventWithFixedStackTrace() {
    Exception e = new Exception("boom");
    e.setStackTrace(new StackTraceElement[] {FRAME_A, FRAME_B, FRAME_C});
    return createLoggingEvent(e);
  }

  private static final String FIXED_STACK_TRACE = "java.lang.Exception: boom" + LS
      + "\tat " + FRAME_A + LS
      + "\tat " + FRAME_B + LS
      + "\tat " + FRAME_C + LS;

  private void restartWithOptions(String... options) {
    tpc.setOptionList(Arrays.asList(options));
    tpc.start();
  }

  @Test
  public void eventWithoutThrowableConvertsToEmptyString() {
    assertEquals(CoreConstants.EMPTY_STRING, tpc.convert(createLoggingEvent(null)));
  }

  @Test
  public void unparsableLengthIsReportedAndWholeTraceIsPrinted() {
    ThrowableProxyConverter converter = new ThrowableProxyConverter();
    converter.setContext(lc);
    converter.setOptionList(Arrays.asList("Bogus"));
    converter.start();

    assertEquals(Integer.MAX_VALUE, converter.lengthOption);
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Could not parse [bogus] as an integer", statuses.get(0).getMessage());
    assertEquals(FIXED_STACK_TRACE, converter.convert(eventWithFixedStackTrace()));
  }

  @Test
  public void optionsNameEitherEvaluatorsOrIgnoredStackTraceLines() {
    StubEventEvaluator e1 = new StubEventEvaluator("e1", false).registerIn(lc);
    StubEventEvaluator e2 = new StubEventEvaluator("e2", false).registerIn(lc);

    restartWithOptions("full", "e1", "skipA", "e2", "skipB");

    assertEquals(2, tpc.evaluatorList.size());
    assertSame(e1, tpc.evaluatorList.get(0));
    assertSame(e2, tpc.evaluatorList.get(1));
    assertEquals(Arrays.asList("skipA", "skipB"), tpc.ignoredStackTraceLines);
  }

  @Test
  public void matchingEvaluatorSuppressesStackTrace() {
    StubEventEvaluator noMatch = new StubEventEvaluator("noMatch", false).registerIn(lc);
    StubEventEvaluator match = new StubEventEvaluator("match", true).registerIn(lc);
    StubEventEvaluator notReached = new StubEventEvaluator("notReached", true).registerIn(lc);
    restartWithOptions("full", "noMatch", "match", "notReached");

    assertEquals(CoreConstants.EMPTY_STRING, tpc.convert(eventWithFixedStackTrace()));
    assertEquals(1, noMatch.invocations);
    assertEquals(1, match.invocations);
    assertEquals("evaluation stops at the first match", 0, notReached.invocations);
  }

  @Test
  public void nonMatchingEvaluatorsLetStackTraceThrough() {
    StubEventEvaluator e1 = new StubEventEvaluator("e1", false).registerIn(lc);
    StubEventEvaluator e2 = new StubEventEvaluator("e2", false).registerIn(lc);
    restartWithOptions("full", "e1", "e2");

    assertEquals(FIXED_STACK_TRACE, tpc.convert(eventWithFixedStackTrace()));
    assertEquals(1, e1.invocations);
    assertEquals(1, e2.invocations);
  }

  @Test
  public void stopDiscardsEvaluators() {
    new StubEventEvaluator("match", true).registerIn(lc);
    restartWithOptions("full", "match");
    assertEquals(CoreConstants.EMPTY_STRING, tpc.convert(eventWithFixedStackTrace()));

    tpc.stop();

    assertFalse(tpc.isStarted());
    assertNull(tpc.evaluatorList);
    assertEquals(FIXED_STACK_TRACE, tpc.convert(eventWithFixedStackTrace()));
  }

  @Test
  public void evaluatorFailuresAreReportedUntilTheErrorLimit() {
    EvaluationException failure = new EvaluationException("boom");
    new StubEventEvaluator("failing", failure).registerIn(lc);
    restartWithOptions("full", "failing");

    for (int i = 0; i < CoreConstants.MAX_ERROR_COUNT + 1; i++) {
      // a failing evaluator does not suppress the stack trace
      assertEquals(FIXED_STACK_TRACE, tpc.convert(eventWithFixedStackTrace()));
    }

    assertEquals(CoreConstants.MAX_ERROR_COUNT + 1, tpc.errorCount);
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), CoreConstants.MAX_ERROR_COUNT, statuses.size());
    for (int i = 0; i < CoreConstants.MAX_ERROR_COUNT - 1; i++) {
      Status status = statuses.get(i);
      assertEquals(Status.ERROR, status.getLevel());
      assertEquals("Exception thrown for evaluator named [failing]", status.getMessage());
      assertSame(failure, status.getThrowable());
      assertSame(tpc, status.getOrigin());
    }
    Status last = statuses.get(CoreConstants.MAX_ERROR_COUNT - 1);
    assertTrue(last instanceof ErrorStatus);
    assertEquals("Exception thrown for evaluator named [failing].", last.getMessage());
    assertSame(failure, last.getThrowable());
    assertSame(tpc, last.getOrigin());
    assertTrue(last.hasChildren());
    Status child = last.iterator().next();
    assertEquals(Status.ERROR, child.getLevel());
    assertEquals("This was the last warning about this evaluator's errors. "
        + "We don't want the StatusManager to get flooded.", child.getMessage());
    assertSame(tpc, child.getOrigin());
  }

  @Test
  public void skippedFramesAreCountedOnTheNextPrintedFrame() {
    restartWithOptions("full", "b.B");

    assertEquals("java.lang.Exception: boom" + LS
        + "\tat " + FRAME_A + LS
        + "\tat " + FRAME_C + " [1 skipped]" + LS, tpc.convert(eventWithFixedStackTrace()));
  }

  @Test
  public void trailingSkippedFramesAreCountedOnTheirOwnLine() {
    restartWithOptions("full", "b.B", "c.C");

    assertEquals("java.lang.Exception: boom" + LS
        + "\tat " + FRAME_A + LS
        + " [2 skipped]" + LS, tpc.convert(eventWithFixedStackTrace()));
  }

  @Test
  public void proxyWithoutSuppressedArrayIsPrinted() {
    IThrowableProxy tp = mock(IThrowableProxy.class);
    when(tp.getClassName()).thenReturn("x.Boom");
    when(tp.getMessage()).thenReturn("msg");
    when(tp.getStackTraceElementProxyArray())
        .thenReturn(new StackTraceElementProxy[] {new StackTraceElementProxy(FRAME_A)});
    when(tp.getSuppressed()).thenReturn(null);
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getThrowableProxy()).thenReturn(tp);

    assertEquals("x.Boom: msg" + LS + "\tat " + FRAME_A + LS, tpc.convert(event));
  }

  void someMethod() throws Exception {
    throw new Exception("someMethod");
  }

  void verify(Throwable t) {
    t.printStackTrace(pw);

    ILoggingEvent le = createLoggingEvent(t);
    String result = tpc.convert(le);
    System.out.println(result);
    result = result.replace("common frames omitted", "more");
    assertEquals(sw.toString(), result);
  }
}
