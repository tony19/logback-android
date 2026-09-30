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
package ch.qos.logback.classic.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class LevelFilterTest {

  private final LoggerContext context = new LoggerContext();
  private final LevelFilter filter = new LevelFilter();

  @Before
  public void setUp() {
    filter.setContext(context);
    filter.setOnMatch(FilterReply.ACCEPT);
    filter.setOnMismatch(FilterReply.DENY);
  }

  private ILoggingEvent eventAt(Level level) {
    return new LoggingEvent(getClass().getName(), context.getLogger("x"), level, "msg", null, null);
  }

  @Test
  public void startedFilterRepliesOnMatchForEventsOfTheConfiguredLevel() {
    filter.setLevel(Level.WARN);
    filter.start();
    assertTrue(filter.isStarted());
    assertEquals(FilterReply.ACCEPT, filter.decide(eventAt(Level.WARN)));
  }

  @Test
  public void startedFilterRepliesOnMismatchForEventsOfOtherLevels() {
    filter.setLevel(Level.WARN);
    filter.start();
    assertEquals(FilterReply.DENY, filter.decide(eventAt(Level.ERROR)));
    assertEquals(FilterReply.DENY, filter.decide(eventAt(Level.INFO)));
  }

  @Test
  public void filterWithoutLevelDoesNotStart() {
    filter.start();
    assertFalse(filter.isStarted());
  }

  @Test
  public void unstartedFilterIsNeutralEvenForMatchingEvents() {
    filter.setLevel(Level.WARN);
    // not started
    assertEquals(FilterReply.NEUTRAL, filter.decide(eventAt(Level.WARN)));
    assertEquals(FilterReply.NEUTRAL, filter.decide(eventAt(Level.INFO)));
  }
}
