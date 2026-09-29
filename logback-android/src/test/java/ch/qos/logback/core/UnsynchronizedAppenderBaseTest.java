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
package ch.qos.logback.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class UnsynchronizedAppenderBaseTest {

  private final Context context = new ContextBase();
  private final StatusChecker statusChecker = new StatusChecker(context);
  private final RecordingAppender appender = new RecordingAppender();

  @Before
  public void setUp() {
    appender.setContext(context);
    appender.setName("recording");
  }

  @Test
  public void doAppendIgnoresEventsLoggedFromWithinAppend() {
    appender.start();

    appender.doAppend("outer");

    // append() re-entered doAppend("inner") on the same thread but the guard dropped it
    assertEquals(Collections.singletonList("outer"), appender.events);
    assertEquals(1, appender.reentrantCalls);
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void guardIsReleasedAfterEachAppend() {
    appender.start();

    appender.doAppend("a");
    appender.doAppend("b");

    assertEquals(Arrays.asList("a", "b"), appender.events);
  }

  @Test
  public void warningsAboutNonStartedAppenderAreCappedAtAllowedRepeats() {
    for (int i = 0; i < UnsynchronizedAppenderBase.ALLOWED_REPEATS + 2; i++) {
      appender.doAppend("event" + i);
    }

    assertTrue(appender.events.isEmpty());
    assertEquals(UnsynchronizedAppenderBase.ALLOWED_REPEATS,
        statusChecker.matchCount("Attempted to append to non started appender \\[recording\\]."));
    assertEquals(Status.WARN, statusChecker.getHighestLevel(0));
  }

  @Test
  public void eventDeniedByFilterIsNotAppended() {
    appender.start();
    appender.addFilter(new FixedReplyFilter(FilterReply.DENY));

    appender.doAppend("denied");

    assertTrue(appender.events.isEmpty());
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void eventAcceptedByFilterIsAppended() {
    appender.start();
    appender.addFilter(new FixedReplyFilter(FilterReply.ACCEPT));

    appender.doAppend("accepted");

    assertEquals(Collections.singletonList("accepted"), appender.events);
  }

  @Test
  public void exceptionThrownByAppendIsReportedAsErrorUpToAllowedRepeats() {
    appender.start();
    appender.failure = new IllegalStateException("boom");

    for (int i = 0; i < UnsynchronizedAppenderBase.ALLOWED_REPEATS + 2; i++) {
      appender.doAppend("event" + i);
    }

    assertEquals(UnsynchronizedAppenderBase.ALLOWED_REPEATS,
        statusChecker.matchCount("Appender \\[recording\\] failed to append."));
    assertEquals(Status.ERROR, statusChecker.getHighestLevel(0));
    statusChecker.asssertContainsException(IllegalStateException.class);
  }

  @Test
  public void guardIsReleasedAfterAppendThrows() {
    appender.start();
    appender.failure = new IllegalStateException("boom");
    appender.doAppend("fails");

    appender.failure = null;
    appender.doAppend("succeeds");

    assertEquals(Arrays.asList("fails", "succeeds"), appender.events);
  }

  @Test
  public void clearAllFiltersRemovesDenyingFilter() {
    appender.start();
    appender.addFilter(new FixedReplyFilter(FilterReply.DENY));
    appender.doAppend("denied");

    appender.clearAllFilters();
    appender.doAppend("accepted");

    assertEquals(Collections.singletonList("accepted"), appender.events);
    assertTrue(appender.getCopyOfAttachedFiltersList().isEmpty());
  }

  @Test
  public void getCopyOfAttachedFiltersListReturnsDetachedCopyOfFilters() {
    Filter<String> filter = new FixedReplyFilter(FilterReply.NEUTRAL);
    appender.addFilter(filter);

    List<Filter<String>> copy = appender.getCopyOfAttachedFiltersList();
    assertEquals(1, copy.size());
    assertSame(filter, copy.get(0));

    copy.clear();
    assertEquals(1, appender.getCopyOfAttachedFiltersList().size());
  }

  /**
   * Records appended events, optionally throws, and, when it sees the "outer"
   * event, logs a nested event through its own {@link #doAppend(Object)}.
   */
  static class RecordingAppender extends UnsynchronizedAppenderBase<String> {
    final List<String> events = new ArrayList<String>();
    int reentrantCalls = 0;
    RuntimeException failure;

    @Override
    protected void append(String event) {
      events.add(event);
      if (failure != null) {
        throw failure;
      }
      if ("outer".equals(event)) {
        reentrantCalls++;
        doAppend("inner");
      }
    }
  }

  static class FixedReplyFilter extends Filter<String> {
    private final FilterReply reply;

    FixedReplyFilter(FilterReply reply) {
      this.reply = reply;
    }

    @Override
    public FilterReply decide(String event) {
      return reply;
    }
  }
}
