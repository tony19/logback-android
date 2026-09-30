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
package ch.qos.logback.core.sift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.Duration;

public class SiftingAppenderBaseTest {

  private final ContextBase context = new ContextBase();

  private final SiftingAppenderBase<Object> appender = new SiftingAppenderBase<Object>() {
    @Override
    protected long getTimestamp(Object event) {
      return 0;
    }

    @Override
    protected boolean eventMarksEndOfLife(Object event) {
      return false;
    }
  };

  @Test
  public void startWithoutDiscriminatorReportsAnErrorInsteadOfThrowing() {
    appender.setContext(context);

    appender.start();

    assertFalse(appender.isStarted());
    assertEquals(Status.ERROR, new StatusUtil(context).getHighestLevel(0));
  }

  @Test
  public void timeoutAndMaxAppenderCountHaveTheTrackerDefaults() {
    assertEquals(AppenderTracker.DEFAULT_TIMEOUT, appender.getTimeout().getMilliseconds());
    assertEquals(AppenderTracker.DEFAULT_MAX_COMPONENTS, appender.getMaxAppenderCount());
  }

  @Test
  public void timeoutAndMaxAppenderCountAreHandedToTheTrackerOnStart() {
    StringSiftingAppender sa = newStringSiftingAppender();
    Duration timeout = Duration.buildBySeconds(5);
    sa.setTimeout(timeout);
    sa.setMaxAppenderCount(3);

    sa.start();

    assertTrue(sa.isStarted());
    assertSame(timeout, sa.getTimeout());
    assertEquals(3, sa.getMaxAppenderCount());
    assertEquals(5000, sa.getAppenderTracker().getTimeout());
    assertEquals(3, sa.getAppenderTracker().getMaxComponents());
  }

  @Test
  public void discriminatorAndItsKeyAreExposed() {
    StringSiftingAppender sa = newStringSiftingAppender();
    assertSame(sa.discriminator, sa.getDiscriminator());
    assertEquals(PrefixDiscriminator.KEY, sa.getDiscriminatorKey());
  }

  @Test
  public void discriminatorKeyIsNullWithoutDiscriminator() {
    assertNull(appender.getDiscriminator());
    assertNull(appender.getDiscriminatorKey());
  }

  @Test
  public void eventsAreDispatchedToTheAppenderOfTheirDiscriminatingValue() {
    StringSiftingAppender sa = newStringSiftingAppender();
    sa.start();

    sa.doAppend("a:1");
    sa.doAppend("b:1");
    sa.doAppend("a:2");

    assertEquals(Arrays.asList("a:1", "a:2"), nestedEvents(sa, "a"));
    assertEquals(Arrays.asList("b:1"), nestedEvents(sa, "b"));
  }

  @Test
  public void eventTimestampsKeepTheirNestedAppenderAliveWhileIdleOnesTimeOut() {
    StringSiftingAppender sa = newStringSiftingAppender();
    sa.setTimeout(Duration.buildByMilliseconds(10000));
    sa.start();
    sa.now = 3000;
    sa.doAppend("idle:1");
    sa.doAppend("busy:1");
    Appender<String> idle = sa.getAppenderTracker().find("idle");
    Appender<String> busy = sa.getAppenderTracker().find("busy");

    // "busy" is used again within the timeout, "idle" is not
    sa.now = 3000 + 9000;
    sa.doAppend("busy:2");
    // past the timeout of "idle" (3000 + 10000) but not of "busy" (12000 + 10000)
    sa.now = 3000 + 10001;
    sa.doAppend("busy:3");

    assertNull(sa.getAppenderTracker().find("idle"));
    assertFalse(idle.isStarted());
    assertSame(busy, sa.getAppenderTracker().find("busy"));
    assertTrue(busy.isStarted());
    assertEquals(Arrays.asList("busy:1", "busy:2", "busy:3"), nestedEvents(sa, "busy"));
  }

  @Test
  public void stopStopsLiveAndLingeringNestedAppenders() {
    StringSiftingAppender sa = newStringSiftingAppender();
    sa.start();
    sa.doAppend("live:1");
    sa.doAppend("lingering:end");
    Appender<String> live = sa.getAppenderTracker().find("live");
    Appender<String> lingering = sa.getAppenderTracker().find("lingering");
    assertTrue(live.isStarted());
    assertTrue(lingering.isStarted());

    sa.stop();

    assertFalse(live.isStarted());
    assertFalse(lingering.isStarted());
  }

  @Test
  public void stopWithoutNestedAppendersIsHarmless() {
    StringSiftingAppender sa = newStringSiftingAppender();
    sa.start();

    sa.stop();

    assertEquals(0, sa.getAppenderTracker().getComponentCount());
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void appendIsIgnoredWhenNotStarted() {
    StringSiftingAppender sa = newStringSiftingAppender();
    sa.discriminator.stop();
    sa.start();
    assertFalse(sa.isStarted());
    // start() still built the tracker, only the discriminator was faulty
    assertNotNull(sa.getAppenderTracker());

    // doAppend() would already refuse the event, so call append() directly
    sa.append("a:1");

    assertEquals(0, sa.getAppenderTracker().getComponentCount());
    assertTrue(sa.factory.builtAppenders.isEmpty());
  }

  private StringSiftingAppender newStringSiftingAppender() {
    StringSiftingAppender sa = new StringSiftingAppender();
    sa.setContext(context);
    return sa;
  }

  private static List<String> nestedEvents(StringSiftingAppender sa, String discriminatingValue) {
    return ((ListAppender<String>) sa.getAppenderTracker().find(discriminatingValue)).list;
  }

  /**
   * Sifts string events of the form "prefix:suffix" by prefix. An event whose
   * suffix is "end" marks the end of life of its nested appender.
   */
  static class StringSiftingAppender extends SiftingAppenderBase<String> {
    final RecordingListAppenderFactory factory = new RecordingListAppenderFactory();
    /** the timestamp of the events appended next */
    long now = 0;

    StringSiftingAppender() {
      PrefixDiscriminator prefixDiscriminator = new PrefixDiscriminator();
      prefixDiscriminator.start();
      setDiscriminator(prefixDiscriminator);
      setAppenderFactory(factory);
    }

    @Override
    protected long getTimestamp(String event) {
      return now;
    }

    @Override
    protected boolean eventMarksEndOfLife(String event) {
      return event.endsWith(":end");
    }
  }

  static class PrefixDiscriminator extends AbstractDiscriminator<String> {
    static final String KEY = "prefix";

    @Override
    public String getDiscriminatingValue(String event) {
      return event.substring(0, event.indexOf(':'));
    }

    @Override
    public String getKey() {
      return KEY;
    }
  }

  static class RecordingListAppenderFactory implements AppenderFactory<String> {
    final List<Appender<String>> builtAppenders = new ArrayList<Appender<String>>();

    @Override
    public Appender<String> buildAppender(Context context, String discriminatingValue) {
      ListAppender<String> la = new ListAppender<String>();
      la.setContext(context);
      la.setName(discriminatingValue);
      la.start();
      builtAppenders.add(la);
      return la;
    }
  }
}
