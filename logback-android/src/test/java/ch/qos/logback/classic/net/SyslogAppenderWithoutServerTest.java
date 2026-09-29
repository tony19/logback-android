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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.SyslogConstants;

/**
 * Unit tests for the parts of {@link SyslogAppender} that need neither a
 * syslog server nor Robolectric: severity mapping and how stack traces are
 * written to the syslog stream. The stream is a recording fake, and the
 * appender is not started, so the stack trace lines carry no syslog prefix.
 * <p>
 * See {@link SyslogAppenderTest} for tests against a (mock) syslog server.
 */
public class SyslogAppenderWithoutServerTest {

  private final Logger logger = new LoggerContext().getLogger("syslog");
  private final SyslogAppender appender = new SyslogAppender();

  @Test
  public void severityFollowsTheEventLevel() {
    assertEquals(SyslogConstants.ERROR_SEVERITY, appender.getSeverityForEvent(event(Level.ERROR, null)));
    assertEquals(SyslogConstants.WARNING_SEVERITY, appender.getSeverityForEvent(event(Level.WARN, null)));
    assertEquals(SyslogConstants.INFO_SEVERITY, appender.getSeverityForEvent(event(Level.INFO, null)));
    assertEquals(SyslogConstants.DEBUG_SEVERITY, appender.getSeverityForEvent(event(Level.DEBUG, null)));
    assertEquals(SyslogConstants.DEBUG_SEVERITY, appender.getSeverityForEvent(event(Level.TRACE, null)));
  }

  @Test
  public void postProcessWritesEachLineOfTheThrowableAndItsCause() {
    RecordingOutputStream out = new RecordingOutputStream(-1);

    appender.postProcess(event(Level.ERROR, outerWithCause()), out);

    assertEquals(Arrays.asList(
        "java.lang.IllegalStateException: outer",
        "at a.B.outer(B.java:1)",
        CoreConstants.CAUSED_BY + "java.io.IOException: inner",
        "at c.D.inner(D.java:2)"), out.writes);
    assertEquals(4, out.flushes);
  }

  @Test
  public void postProcessStopsAtTheFirstWriteFailure() {
    RecordingOutputStream out = new RecordingOutputStream(0);

    appender.postProcess(event(Level.ERROR, outerWithCause()), out);

    assertEquals(1, out.attempts);
    assertTrue(out.writes.isEmpty());
  }

  @Test
  public void postProcessStopsWhenAStackFrameCannotBeWritten() {
    RecordingOutputStream out = new RecordingOutputStream(1);

    appender.postProcess(event(Level.ERROR, outerWithCause()), out);

    // the cause is not written once writing the outer exception failed
    assertEquals(2, out.attempts);
    assertEquals(Arrays.asList("java.lang.IllegalStateException: outer"), out.writes);
  }

  @Test
  public void postProcessWritesNothingWhenThrowablesAreExcluded() {
    assertFalse(appender.isThrowableExcluded());
    appender.setThrowableExcluded(true);
    assertTrue(appender.isThrowableExcluded());
    RecordingOutputStream out = new RecordingOutputStream(-1);

    appender.postProcess(event(Level.ERROR, outerWithCause()), out);

    assertEquals(0, out.attempts);
  }

  @Test
  public void postProcessWritesNothingForAnEventWithoutThrowable() {
    RecordingOutputStream out = new RecordingOutputStream(-1);

    appender.postProcess(event(Level.ERROR, null), out);

    assertEquals(0, out.attempts);
  }

  @Test
  public void stackTracePatternDefaultsToATab() {
    assertEquals("\t", appender.getStackTracePattern());
    appender.setStackTracePattern("[%thread] ");
    assertEquals("[%thread] ", appender.getStackTracePattern());
  }

  private LoggingEvent event(Level level, Throwable throwable) {
    return new LoggingEvent(getClass().getName(), logger, level, "message", throwable, null);
  }

  private static Throwable outerWithCause() {
    Exception inner = new IOException("inner");
    inner.setStackTrace(new StackTraceElement[] {new StackTraceElement("c.D", "inner", "D.java", 2)});
    Exception outer = new IllegalStateException("outer", inner);
    outer.setStackTrace(new StackTraceElement[] {new StackTraceElement("a.B", "outer", "B.java", 1)});
    return outer;
  }

  /**
   * Records each write as a separate string; fails (with an
   * {@link IOException}) from the write with the given index on, or never
   * when the index is negative.
   */
  private static class RecordingOutputStream extends OutputStream {
    private final int failFrom;
    final List<String> writes = new ArrayList<String>();
    int attempts;
    int flushes;

    RecordingOutputStream(int failFrom) {
      this.failFrom = failFrom;
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      int attempt = attempts++;
      if (failFrom >= 0 && attempt >= failFrom) {
        throw new IOException("write failed");
      }
      writes.add(new String(b, off, len, StandardCharsets.UTF_8));
    }

    @Override
    public void flush() {
      flushes++;
    }
  }
}
