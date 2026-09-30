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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.testUtil.RandomUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Relatively straightforward unit tests for AppenderTracker.
 */
public class AppenderTrackerTest {

  Context context = new ContextBase();
  ListAppenderFactory listAppenderFactory = new ListAppenderFactory();
  int diff = RandomUtil.getPositiveInt();
  AppenderTracker<Object> appenderTracker = new AppenderTracker<Object>(context, listAppenderFactory);
  String key = "k-" + diff;
  long now = 3000;

  @Before
  public void setUp() {
  }

  @Test
  public void removeStaleComponentsShouldNotBomb() {
    appenderTracker.removeStaleComponents(now);
    assertEquals(0, appenderTracker.getComponentCount());
  }

  @Test
  public void findingTheInexistentShouldNotBomb() {
    assertNull(appenderTracker.find(key));
    now += AppenderTracker.DEFAULT_TIMEOUT + 1;
    appenderTracker.removeStaleComponents(now);
    assertNull(appenderTracker.find(key));
  }

  @Test
  public void smoke() {
    Appender<Object> a = appenderTracker.getOrCreate(key, now);
    assertTrue(a.isStarted());
    now += AppenderTracker.DEFAULT_TIMEOUT + 1;
    appenderTracker.removeStaleComponents(now);
    assertFalse(a.isStarted());
    assertNull(appenderTracker.find(key));
  }

  @Test
  public void endOfLivedAppendersShouldBeRemovedAfterLingeringTimeout() {
    Appender<Object> a = appenderTracker.getOrCreate(key, now);
    appenderTracker.endOfLife(key);
    now += AppenderTracker.LINGERING_TIMEOUT + 1;
    appenderTracker.removeStaleComponents(now);
    assertFalse(a.isStarted());
    a = appenderTracker.find(key);
    assertNull(a);
  }

  @Test
  public void endOfLivedAppenderShouldBeAvailableDuringLingeringPeriod() {
    Appender<Object> a = appenderTracker.getOrCreate(key, now);
    appenderTracker.endOfLife(key);
    // clean
    appenderTracker.removeStaleComponents(now);
    Appender<Object> lingering = appenderTracker.getOrCreate(key, now);
    assertTrue(lingering.isStarted());
    assertTrue(a == lingering);
    now += AppenderTracker.LINGERING_TIMEOUT + 1;
    appenderTracker.removeStaleComponents(now);
    assertFalse(a.isStarted());
    a = appenderTracker.find(key);
    assertNull(a);
  }


  @Test
  public void trackerShouldHonorMaxComponentsParameter() {
    List<Appender<Object>> appenderList = new ArrayList<Appender<Object>>();
    int max = 10;
    appenderTracker.setMaxComponents(max);
    for (int i = 0; i < (max + 1); i++) {
      Appender<Object> a = appenderTracker.getOrCreate(key + "-" + i, now++);
      appenderList.add(a);
    }
    // cleaning only happens in removeStaleComponents
    appenderTracker.removeStaleComponents(now++);
    assertEquals(max, appenderTracker.allKeys().size());
    assertNull(appenderTracker.find(key + "-" + 0));
    assertFalse(appenderList.get(0).isStarted());
  }

  @Test
  public void trackerShouldHonorTimeoutParameter() {
    List<Appender<Object>> appenderList = new ArrayList<Appender<Object>>();
    int timeout = 2;
    appenderTracker.setTimeout(timeout);
    for (int i = 0; i <= timeout; i++) {
      Appender<Object> a = appenderTracker.getOrCreate(key + "-" + i, now++);
      appenderList.add(a);
    }

    long numComponentsCreated = timeout + 1;
    assertEquals(numComponentsCreated, appenderTracker.allKeys().size());

    // cleaning only happens in removeStaleComponents. The first appender should timeout
    appenderTracker.removeStaleComponents(now++);

    // the first appender should have been removed
    assertEquals(numComponentsCreated - 1, appenderTracker.allKeys().size());
    assertNull(appenderTracker.find(key + "-" + 0));
    assertFalse(appenderList.get(0).isStarted());

    // the other appenders should be in the tracker
    for (int i = 1; i <= timeout; i++) {
      assertNotNull(appenderTracker.find(key + "-" + i));
      assertTrue(appenderList.get(i).isStarted());
    }
  }

  @Test
  public void stoppedAppenderIsRemovedBeforeItTimesOut() {
    Appender<Object> stopped = appenderTracker.getOrCreate(key + "-stopped", now);
    Appender<Object> running = appenderTracker.getOrCreate(key + "-running", now);
    stopped.stop();

    // well before DEFAULT_TIMEOUT
    appenderTracker.removeStaleComponents(now);

    assertNull(appenderTracker.find(key + "-stopped"));
    assertSame(running, appenderTracker.find(key + "-running"));
    assertTrue(running.isStarted());
  }

  @Test
  public void failureToBuildAnAppenderIsReportedAndYieldsAStartedNOPAppender() {
    AppenderTracker<Object> tracker = new AppenderTracker<Object>(context, new FailingAppenderFactory());

    Appender<Object> appender = tracker.getOrCreate(key, now);

    assertTrue(appender instanceof NOPAppender);
    assertTrue(appender.isStarted());
    assertSame(context, appender.getContext());
    assertSame(appender, tracker.find(key));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(2, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Error while building appender with discriminating value [" + key + "]",
        statuses.get(0).getMessage());
    assertSame(tracker, statuses.get(0).getOrigin());
    assertEquals(Status.ERROR, statuses.get(1).getLevel());
    assertEquals("Building NOPAppender for discriminating value [" + key + "]",
        statuses.get(1).getMessage());
  }

  @Test
  public void nopAppenderErrorsAreReportedAtMostMaxErrorCountTimes() {
    AppenderTracker<Object> tracker = new AppenderTracker<Object>(context, new NullAppenderFactory());

    int attempts = CoreConstants.MAX_ERROR_COUNT + 3;
    for (int i = 0; i < attempts; i++) {
      assertTrue(tracker.getOrCreate(key + "-" + i, now) instanceof NOPAppender);
    }

    assertEquals(attempts, tracker.getComponentCount());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(CoreConstants.MAX_ERROR_COUNT, statuses.size());
    for (int i = 0; i < CoreConstants.MAX_ERROR_COUNT; i++) {
      assertEquals("Building NOPAppender for discriminating value [" + key + "-" + i + "]",
          statuses.get(i).getMessage());
    }
  }

  // ======================================================================
  static class FailingAppenderFactory implements AppenderFactory<Object> {
    public Appender<Object> buildAppender(Context context, String discriminatingValue) throws JoranException {
      throw new JoranException("cannot build " + discriminatingValue);
    }
  }

  static class NullAppenderFactory implements AppenderFactory<Object> {
    public Appender<Object> buildAppender(Context context, String discriminatingValue) {
      return null;
    }
  }

  static class ListAppenderFactory implements AppenderFactory<Object> {

    public Appender<Object> buildAppender(Context context, String discriminatingValue) throws JoranException {
      ListAppender<Object> la = new ListAppender<Object>();
      la.setContext(context);
      la.setName(discriminatingValue);
      la.start();
      return la;
    }
  }
}
