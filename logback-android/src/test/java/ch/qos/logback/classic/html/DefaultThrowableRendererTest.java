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
package ch.qos.logback.classic.html;

import static ch.qos.logback.classic.html.DefaultThrowableRenderer.TRACE_PREFIX;
import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import ch.qos.logback.classic.spi.DummyThrowableProxy;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

public class DefaultThrowableRendererTest {

  private final DefaultThrowableRenderer renderer = new DefaultThrowableRenderer();

  @Test
  public void throwableWithoutCommonFramesListsEveryFrameWithoutCausedByOrOmissionNote() {
    DummyThrowableProxy tp = proxy("java.lang.IllegalStateException", "bad <state>", 0,
        frame("c1", "m1", 1), frame("c2", "m2", 2));

    StringBuilder sb = new StringBuilder();
    renderer.render(sb, tp);

    assertEquals("java.lang.IllegalStateException: bad &lt;state&gt;" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c1.m1(f1:1)" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c2.m2(f2:2)" + LINE_SEPARATOR, sb.toString());
  }

  @Test
  public void throwableSharingFramesIsMarkedAsCauseAndSummarizesTheOmittedFrames() {
    DummyThrowableProxy tp = proxy("java.io.IOException", "disk full", 2,
        frame("c1", "m1", 1), frame("c2", "m2", 2), frame("c3", "m3", 3));

    StringBuilder sb = new StringBuilder();
    renderer.render(sb, tp);

    assertEquals("<br />Caused by: java.io.IOException: disk full" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c1.m1(f1:1)" + LINE_SEPARATOR
        + TRACE_PREFIX + "\t... 2 common frames omitted" + LINE_SEPARATOR, sb.toString());
  }

  @Test
  public void printFirstLinePrefixesCausedByOnlyWhenFramesAreShared() {
    StringBuilder top = new StringBuilder();
    renderer.printFirstLine(top, proxy("a.Top", "top", 0));
    assertEquals("a.Top: top" + LINE_SEPARATOR, top.toString());

    StringBuilder cause = new StringBuilder();
    renderer.printFirstLine(cause, proxy("a.Cause", "cause", 1));
    assertEquals("<br />Caused by: a.Cause: cause" + LINE_SEPARATOR, cause.toString());
  }

  @Test
  public void eventRendersWholeCauseChainInsideOneExceptionRow() {
    DummyThrowableProxy cause = proxy("java.io.IOException", "inner", 1,
        frame("c2", "m2", 2), frame("c3", "m3", 3));
    DummyThrowableProxy top = proxy("java.lang.RuntimeException", "outer", 0,
        frame("c1", "m1", 1), frame("c3", "m3", 3));
    top.setCause(cause);
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getThrowableProxy()).thenReturn(top);

    StringBuilder sb = new StringBuilder();
    renderer.render(sb, event);

    assertEquals("<tr><td class=\"Exception\" colspan=\"6\">"
        + "java.lang.RuntimeException: outer" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c1.m1(f1:1)" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c3.m3(f3:3)" + LINE_SEPARATOR
        + "<br />Caused by: java.io.IOException: inner" + LINE_SEPARATOR
        + TRACE_PREFIX + "at c2.m2(f2:2)" + LINE_SEPARATOR
        + TRACE_PREFIX + "\t... 1 common frames omitted" + LINE_SEPARATOR
        + "</td></tr>", sb.toString());
  }

  private static DummyThrowableProxy proxy(String className, String message, int commonFrames,
      StackTraceElementProxy... frames) {
    DummyThrowableProxy tp = new DummyThrowableProxy();
    tp.setClassName(className);
    tp.setMessage(message);
    tp.setCommonFramesCount(commonFrames);
    tp.setStackTraceElementProxyArray(frames);
    return tp;
  }

  private static StackTraceElementProxy frame(String className, String method, int line) {
    return new StackTraceElementProxy(
        new StackTraceElement(className, method, "f" + line, line));
  }
}
