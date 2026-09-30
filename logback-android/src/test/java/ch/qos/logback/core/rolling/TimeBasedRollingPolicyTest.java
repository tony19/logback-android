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
package ch.qos.logback.core.rolling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.FileSize;

public class TimeBasedRollingPolicyTest {

  // 2020-06-15T12:00:00Z
  static final long NOW = 1592222400000L;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  ContextBase context = new ContextBase();
  StatusChecker checker = new StatusChecker(context);
  RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  TimeBasedRollingPolicy<Object> tbrp = new TimeBasedRollingPolicy<Object>();
  DefaultTimeBasedFileNamingAndTriggeringPolicy<Object> fnatp = new DefaultTimeBasedFileNamingAndTriggeringPolicy<Object>();

  @Before
  public void setUp() {
    rfa.setContext(context);
    tbrp.setContext(context);
    tbrp.setParent(rfa);
    fnatp.setCurrentTime(NOW);
    tbrp.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
  }

  @After
  public void tearDown() {
    // stops the executor running asynchronous clean-up jobs, if any was started
    context.stop();
  }

  private String path(String name) {
    return new File(tmp.getRoot(), name).getAbsolutePath();
  }

  private Status lastStatus() {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    return statuses.get(statuses.size() - 1);
  }

  private void startDaily() {
    tbrp.setFileNamePattern(path("app-%d{yyyy-MM-dd, UTC}.log"));
    tbrp.start();
    assertTrue(tbrp.isStarted());
  }

  @SuppressWarnings("unchecked")
  private static Future<Object> futureFailingWith(Exception failure) throws Exception {
    Future<Object> future = mock(Future.class);
    when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(failure);
    return future;
  }

  @Test
  public void startWithoutFileNamePatternIsRejected() {
    IllegalStateException e = assertThrows(IllegalStateException.class, tbrp::start);

    assertEquals(TimeBasedRollingPolicy.FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET, e.getMessage());
    checker.assertContainsMatch(Status.WARN, Pattern.quote(TimeBasedRollingPolicy.FNP_NOT_SET));
    checker.assertContainsMatch(Status.WARN, Pattern.quote(CoreConstants.SEE_FNP_NOT_SET));
    assertFalse(tbrp.isStarted());
  }

  @Test
  public void historyIsUnboundedAndNotCleanedOnStartByDefault() {
    assertEquals(CoreConstants.UNBOUND_HISTORY, tbrp.getMaxHistory());
    assertFalse(tbrp.isCleanHistoryOnStart());
  }

  @Test
  public void cleanHistoryOnStartRemovesArchivesOlderThanMaxHistory() throws Exception {
    File expired = tmp.newFile("app-2020-06-01.log");
    File retained = tmp.newFile("app-2020-06-14.log");
    tbrp.setMaxHistory(2);
    tbrp.setCleanHistoryOnStart(true);

    startDaily();

    assertEquals(2, tbrp.getMaxHistory());
    assertTrue(tbrp.isCleanHistoryOnStart());
    checker.assertContainsMatch(Status.INFO, "Cleaning on start up");
    assertNotNull(tbrp.cleanUpFuture);
    tbrp.cleanUpFuture.get(CoreConstants.SECONDS_TO_WAIT_FOR_COMPRESSION_JOBS, TimeUnit.SECONDS);
    assertFalse(expired.exists());
    assertTrue(retained.exists());
  }

  @Test
  public void maxHistoryWithoutCleanHistoryOnStartLeavesArchivesUntilRollover() throws IOException {
    File expired = tmp.newFile("app-2020-06-01.log");
    tbrp.setMaxHistory(2);

    startDaily();

    assertNull(tbrp.cleanUpFuture);
    assertTrue(expired.exists());
  }

  @Test
  public void totalSizeCapWithoutMaxHistoryIsIgnored() {
    tbrp.setTotalSizeCap(new FileSize(1024));

    startDaily();

    checker.assertContainsMatch(Status.WARN,
        Pattern.quote("'maxHistory' is not set, ignoring 'totalSizeCap' option with value [1 KB]"));
  }

  @Test
  public void stopReportsCompressionJobThatTimesOut() throws Exception {
    startDaily();
    TimeoutException timeout = new TimeoutException();
    tbrp.compressionFuture = futureFailingWith(timeout);

    tbrp.stop();

    assertFalse(tbrp.isStarted());
    Status status = lastStatus();
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("Timeout while waiting for compression job to finish", status.getMessage());
    assertSame(timeout, status.getThrowable());
  }

  @Test
  public void stopReportsCleanUpJobThatFails() throws Exception {
    startDaily();
    ExecutionException failure = new ExecutionException(new IOException("cannot delete"));
    tbrp.cleanUpFuture = futureFailingWith(failure);

    tbrp.stop();

    assertFalse(tbrp.isStarted());
    Status status = lastStatus();
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("Unexpected exception while waiting for clean-up job to finish", status.getMessage());
    assertSame(failure, status.getThrowable());
  }
}
