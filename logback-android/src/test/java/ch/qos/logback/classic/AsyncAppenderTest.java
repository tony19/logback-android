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
package ch.qos.logback.classic;

import ch.qos.logback.classic.net.testObjectBuilders.LoggingEventBuilderInContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.testUtil.RandomUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.junit.Assert.*;

/**
 * @author Ceki G&uuml;lc&uuml;
 * @author Torsten Juergeleit
 */
public class AsyncAppenderTest {

  String thisClassName = this.getClass().getName();
  LoggerContext context = new LoggerContext();
  AsyncAppender asyncAppender = new AsyncAppender();
  ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>();
  LoggingEventBuilderInContext builder = new LoggingEventBuilderInContext(context, thisClassName, UnsynchronizedAppenderBase.class.getName());
  int diff = RandomUtil.getPositiveInt();

  @Before
  public void setUp() {
    asyncAppender.setContext(context);
    listAppender.setContext(context);
    listAppender.setName("list");
    listAppender.start();
  }

  @Test
  public void eventWasPreparedForDeferredProcessing() {
    asyncAppender.addAppender(listAppender);
    asyncAppender.start();

    String k = "k" + diff;
    MDC.put(k, "v");
    asyncAppender.doAppend(builder.build(diff));
    MDC.clear();

    asyncAppender.stop();
    assertFalse(asyncAppender.isStarted());

    // check the event
    assertEquals(1, listAppender.list.size());
    ILoggingEvent e = listAppender.list.get(0);

    // check that MDC values were correctly retained
    assertEquals("v", e.getMDCPropertyMap().get(k));
    assertFalse(e.hasCallerData());
  }

  @Test
  public void settingIncludeCallerDataPropertyCausedCallerDataToBeIncluded() {
    asyncAppender.addAppender(listAppender);
    asyncAppender.setIncludeCallerData(true);
    asyncAppender.start();


    asyncAppender.doAppend(builder.build(diff));
    asyncAppender.stop();

    // check the event
    assertEquals(1, listAppender.list.size());
    ILoggingEvent e = listAppender.list.get(0);
    assertTrue(e.hasCallerData());
    StackTraceElement ste = e.getCallerData()[0];
    assertEquals(thisClassName, ste.getClassName());
  }

  private ILoggingEvent eventAt(Level level) {
    return new LoggingEvent(thisClassName, context.getLogger(thisClassName), level, level.toString(), null, null);
  }

  @Test
  public void eventsOfLevelInfoOrLowerAreDiscardable() {
    assertTrue(asyncAppender.isDiscardable(eventAt(Level.TRACE)));
    assertTrue(asyncAppender.isDiscardable(eventAt(Level.DEBUG)));
    assertTrue(asyncAppender.isDiscardable(eventAt(Level.INFO)));
    assertFalse(asyncAppender.isDiscardable(eventAt(Level.WARN)));
    assertFalse(asyncAppender.isDiscardable(eventAt(Level.ERROR)));
  }

  @Test
  public void discardableEventsAreDroppedWhenQueueIsBelowDiscardingThreshold() {
    asyncAppender.addAppender(listAppender);
    asyncAppender.setQueueSize(10);
    // remaining capacity (at most 10) is always below this threshold
    asyncAppender.setDiscardingThreshold(11);
    // stop() returns as soon as the worker has flushed; a generous bound keeps a slow machine from losing events
    asyncAppender.setMaxFlushTime(60000);
    asyncAppender.start();

    for (Level level : new Level[] {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR}) {
      asyncAppender.doAppend(eventAt(level));
    }
    asyncAppender.stop();

    assertEquals(2, listAppender.list.size());
    assertEquals(Level.WARN, listAppender.list.get(0).getLevel());
    assertEquals(Level.ERROR, listAppender.list.get(1).getLevel());
  }

  @Test
  public void includeCallerDataIsOffByDefaultAndSettable() {
    assertFalse(asyncAppender.isIncludeCallerData());
    asyncAppender.setIncludeCallerData(true);
    assertTrue(asyncAppender.isIncludeCallerData());
  }
}
