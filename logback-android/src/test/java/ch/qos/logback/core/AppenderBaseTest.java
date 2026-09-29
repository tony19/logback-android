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

public class AppenderBaseTest {

  private final Context context = new ContextBase();
  private final StatusChecker statusChecker = new StatusChecker(context);
  private final ReentrantListAppender appender = new ReentrantListAppender();

  @Before
  public void setUp() {
    appender.setContext(context);
    appender.setName("reentrant");
  }

  @Test
  public void doAppendIgnoresEventsLoggedFromWithinAppend() {
    appender.start();

    appender.doAppend("outer");

    // append() re-entered doAppend("inner") but the guard dropped it
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
    for (int i = 0; i < AppenderBase.ALLOWED_REPEATS + 3; i++) {
      appender.doAppend("event" + i);
    }

    assertTrue(appender.events.isEmpty());
    assertEquals(AppenderBase.ALLOWED_REPEATS,
        statusChecker.matchCount("Attempted to append to non started appender \\[reentrant\\]."));
    assertEquals(Status.WARN, statusChecker.getHighestLevel(0));
  }

  @Test
  public void clearAllFiltersRemovesDenyingFilter() {
    appender.start();
    appender.addFilter(new FixedReplyFilter(FilterReply.DENY));

    appender.doAppend("denied");
    assertTrue(appender.events.isEmpty());

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
   * Records appended events and, when it sees the "outer" event, logs a
   * nested event through its own {@link #doAppend(Object)}.
   */
  static class ReentrantListAppender extends AppenderBase<String> {
    final List<String> events = new ArrayList<String>();
    int reentrantCalls = 0;

    @Override
    protected void append(String event) {
      events.add(event);
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
