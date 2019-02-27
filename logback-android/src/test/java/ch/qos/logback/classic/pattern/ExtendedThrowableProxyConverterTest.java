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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class ExtendedThrowableProxyConverterTest {

  LoggerContext lc = new LoggerContext();
  ExtendedThrowableProxyConverter etpc = new ExtendedThrowableProxyConverter();
  StringWriter sw = new StringWriter();
  PrintWriter pw = new PrintWriter(sw);

  @Before
  public void setUp() throws Exception {
    lc.setPackagingDataEnabled(true);
    etpc.setContext(lc);
    etpc.start();
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
  public void integration() {
    PatternLayout pl = new PatternLayout();
    pl.setContext(lc);
    pl.setPattern("%m%n");
    pl.start();
    ILoggingEvent e = createLoggingEvent(new Exception("x"));
    String res = pl.doLayout(e);

    // make sure that at least some package data was output
    Pattern p = Pattern.compile("\\s*at .*?\\[.*?\\]");
    Matcher m = p.matcher(res);
    int i = 0;
    while(m.find()) {
      i++;
    }
    assertThat(i, greaterThan(5));
  }

  @Test
  public void smoke() {
    Exception t = new Exception("smoke");
    verify(t);
  }

  @Test
  public void nested() {
    Throwable t = makeNestedException(1);
    verify(t);
  }

  void verify(Throwable t) {
    t.printStackTrace(pw);

    ILoggingEvent le = createLoggingEvent(t);
    String result = etpc.convert(le);
    result = result.replace("common frames omitted", "more");
    result = result.replaceAll(" ~?\\[.*\\]", "");
    assertEquals(sw.toString(), result);
  }

  Throwable makeNestedException(int level) {
    if (level == 0) {
      return new Exception("nesting level=" + level);
    }
    Throwable cause = makeNestedException(level - 1);
    return new Exception("nesting level =" + level, cause);
  }
}
