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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;

import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.qos.logback.classic.util.TestHelper.makeNestedException;
import static ch.qos.logback.classic.util.TestHelper.positionOf;
//import static org.fest.assertions.Assertions.assertThat;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tomasz Nurkiewicz
 * @since 2010-08-15, 18:34:21
 */
public class RootCauseFirstThrowableProxyConverterTest {

  private LoggerContext context = new LoggerContext();
  private ThrowableProxyConverter converter = new RootCauseFirstThrowableProxyConverter();
  private StringWriter stringWriter = new StringWriter();
  private PrintWriter printWriter = new PrintWriter(stringWriter);

  @Before
  public void setUp() throws Exception {
    converter.setContext(context);
    converter.start();
  }

  private ILoggingEvent createLoggingEvent(Throwable t) {
    return new LoggingEvent(this.getClass().getName(), context
        .getLogger(Logger.ROOT_LOGGER_NAME), Level.DEBUG, "test message", t,
        null);
  }

  @Test
  public void integration() {
    //given
    context.setPackagingDataEnabled(true);
    PatternLayout pl = new PatternLayout();
    pl.setContext(context);
    pl.setPattern("%m%rEx%n");
    pl.start();

    //when
    ILoggingEvent e = createLoggingEvent(new Exception("x"));
    String result = pl.doLayout(e);

    //then
    // make sure that at least some package data was output
    Pattern p = Pattern.compile("\\s*at .*?\\[.*?\\]");
    Matcher m = p.matcher(result);
    int i = 0;
    while(m.find()) {
      i++;
    }
    //assertThat(i).isGreaterThan(5);
    assertTrue(i > 0);
  }

  @Test
  public void smoke() {
    //given
    Exception exception = new Exception("smoke");
    exception.printStackTrace(printWriter);

    //when
    ILoggingEvent le = createLoggingEvent(exception);
    String result = converter.convert(le);

    //then
    result = result.replace("common frames omitted", "more");
    result = result.replaceAll(" ~?\\[.*\\]", "");
    //assertThat(result).isEqualTo(stringWriter.toString());
    assertEquals(stringWriter.toString(), result);    
  }

  @Test
  public void nested() {
    //given
    Throwable nestedException = makeNestedException(2);
    nestedException.printStackTrace(printWriter);

    //when
    ILoggingEvent le = createLoggingEvent(nestedException);
    String result = converter.convert(le);

    //then
//    assertThat(result).startsWith("java.lang.Exception: nesting level=0");
//    assertThat(
//            positionOf("nesting level=0").in(result)).isLessThan(
//            positionOf("nesting level =1").in(result));
//    assertThat(
//            positionOf("nesting level =1").in(result)).isLessThan(
//            positionOf("nesting level =2").in(result));
    assertTrue(result.startsWith("java.lang.Exception: nesting level=0"));
    assertTrue(positionOf("nesting level=0").in(result) < positionOf("nesting level =1").in(result));
    assertTrue(positionOf("nesting level =1").in(result) < positionOf("nesting level =2").in(result));
  }

  @Test
  public void suppressedExceptionFollowsItsParentWithPrefix() {
    //given
    context.setPackagingDataEnabled(false);
    StackTraceElement outerFrame = new StackTraceElement("a.A", "m", "A.java", 1);
    StackTraceElement suppressedFrame = new StackTraceElement("s.S", "m", "S.java", 5);
    Exception outer = new Exception("outer");
    outer.setStackTrace(new StackTraceElement[] {outerFrame});
    Exception suppressed = new Exception("suppressed");
    suppressed.setStackTrace(new StackTraceElement[] {suppressedFrame, outerFrame});
    outer.addSuppressed(suppressed);

    //when
    String result = converter.convert(createLoggingEvent(outer));

    //then
    String ls = CoreConstants.LINE_SEPARATOR;
    assertEquals("java.lang.Exception: outer" + ls
        + "\tat " + outerFrame + ls
        + "\tSuppressed: java.lang.Exception: suppressed" + ls
        + "\t\tat " + suppressedFrame + ls
        + "\t\t... 1 common frames omitted" + ls, result);
  }

  @Test
  public void proxyWithoutSuppressedArrayIsPrinted() {
    //given
    StackTraceElement frame = new StackTraceElement("a.A", "m", "A.java", 1);
    IThrowableProxy tp = mock(IThrowableProxy.class);
    when(tp.getClassName()).thenReturn("x.Boom");
    when(tp.getMessage()).thenReturn("msg");
    when(tp.getStackTraceElementProxyArray())
        .thenReturn(new StackTraceElementProxy[] {new StackTraceElementProxy(frame)});
    when(tp.getSuppressed()).thenReturn(null);
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getThrowableProxy()).thenReturn(tp);

    //when
    String result = converter.convert(event);

    //then
    String ls = CoreConstants.LINE_SEPARATOR;
    assertEquals("x.Boom: msg" + ls + "\tat " + frame + ls, result);
  }
}
