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
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.TestHelper;
import ch.qos.logback.core.CoreConstants;

import static ch.qos.logback.classic.util.TestHelper.addSuppressed;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

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
