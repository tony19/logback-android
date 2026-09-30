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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.appender.AbstractAppenderTest;
import ch.qos.logback.core.encoder.DummyEncoder;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.LockProbingOutputStream;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.StatusPrinter;

public class RollingFileAppenderTest extends AbstractAppenderTest<Object> {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  Context context = new ContextBase();

  TimeBasedRollingPolicy<Object> tbrp = new TimeBasedRollingPolicy<Object>();
  int diff = RandomUtil.getPositiveInt();
  String randomOutputDir = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "/";

  @Before
  public void setUp() throws Exception {
    // noStartTest fails if the context is set in setUp
    // rfa.setContext(context);

    rfa.setEncoder(new DummyEncoder<Object>());
    rfa.setName("test");
    tbrp.setContext(context);
    tbrp.setParent(rfa);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Override
  protected Appender<Object> getAppender() {
    return rfa;
  }

  @Override
  protected Appender<Object> getConfiguredAppender() {
    rfa.setContext(context);
    tbrp
            .setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%d.log");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);

    rfa.start();
    return rfa;
  }

  @Test
  public void testPrudentModeLogicalImplications() {
    rfa.setContext(context);
    // prudent mode will force "file" property to be null
    rfa.setFile("some non null value");
    rfa.setAppend(false);
    rfa.setPrudent(true);

    tbrp
            .setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%d.log");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);

    rfa.start();

    assertTrue(rfa.isAppend());
    assertNull(rfa.rawFileProperty());
    assertTrue(rfa.isStarted());
  }

  @Test
  public void testPrudentModeLogicalImplicationsOnCompression() {
    rfa.setContext(context);
    rfa.setAppend(false);
    rfa.setPrudent(true);

    tbrp.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%d.log.zip");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);

    rfa.start();

    StatusChecker checker = new StatusChecker(context);
    assertFalse(rfa.isStarted());
    assertEquals(Status.ERROR, checker.getHighestLevel(0));
  }

  @Test
  public void testFilePropertyAfterRollingPolicy() {
    rfa.setContext(context);
    rfa.setRollingPolicy(tbrp);
    rfa.setFile("x");
    StatusPrinter.print(context);
    StatusChecker statusChecker = new StatusChecker(context.getStatusManager());
    statusChecker.assertContainsMatch(Status.ERROR,
            "File property must be set before any triggeringPolicy ");
  }

  @Test
  public void testFilePropertyAfterTriggeringPolicy() {
    rfa.setContext(context);
    rfa.setTriggeringPolicy(new SizeBasedTriggeringPolicy<Object>());
    rfa.setFile("x");
    StatusChecker statusChecker = new StatusChecker(context.getStatusManager());
    statusChecker.assertContainsMatch(Status.ERROR,
            "File property must be set before any triggeringPolicy ");
  }

  @Test
  public void testFileNameWithParenthesis() {
    // if ')' is not escaped, the test throws
    // java.lang.IllegalStateException: FileNamePattern [.../program(x86)/toto-%d.log] does not contain a valid DateToken
    rfa.setContext(context);
    tbrp
            .setFileNamePattern(randomOutputDir + "program(x86)/toto-%d.log");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);
    rfa.start();
    rfa.doAppend("hello");
  }

  @Test
  public void stopTimeBasedRollingPolicy() {
    rfa.setContext(context);

    tbrp.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%d.log.zip");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);
    rfa.start();

    StatusPrinter.print(context);
    assertTrue(tbrp.isStarted());
    assertTrue(rfa.isStarted());
    rfa.stop();
    assertFalse(rfa.isStarted());
    assertFalse(tbrp.isStarted());

  }

  @Test
  public void stopFixedWindowRollingPolicy() {
    rfa.setContext(context);
    rfa.setFile(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-.log");

    FixedWindowRollingPolicy fwRollingPolicy = new FixedWindowRollingPolicy();
    fwRollingPolicy.setContext(context);
    fwRollingPolicy.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%i.log.zip");
    fwRollingPolicy.setParent(rfa);
    fwRollingPolicy.start();
    SizeBasedTriggeringPolicy<Object> sbTriggeringPolicy = new SizeBasedTriggeringPolicy<Object>();
    sbTriggeringPolicy.setContext(context);
    sbTriggeringPolicy.start();

    rfa.setRollingPolicy(fwRollingPolicy);
    rfa.setTriggeringPolicy(sbTriggeringPolicy);

    rfa.start();

    StatusPrinter.print(context);
    assertTrue(fwRollingPolicy.isStarted());
    assertTrue(sbTriggeringPolicy.isStarted());
    assertTrue(rfa.isStarted());
    rfa.stop();
    assertFalse(rfa.isStarted());
    assertFalse(fwRollingPolicy.isStarted());
    assertFalse(sbTriggeringPolicy.isStarted());

  }

  /**
   * Test for http://jira.qos.ch/browse/LOGBACK-796
   */
  @Test
  public void testFileShouldNotMatchFileNamePattern() {
    rfa.setContext(context);
    rfa.setFile(CoreTestConstants.OUTPUT_DIR_PREFIX + "x-2013-04.log");
    tbrp.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "x-%d{yyyy-MM}.log");
    tbrp.start();

    rfa.setRollingPolicy(tbrp);
    rfa.start();
    StatusChecker statusChecker = new StatusChecker(context);
    final String msg = "File property collides with fileNamePattern. Aborting.";
    boolean containsMatch = statusChecker.containsMatch(Status.ERROR, msg);
    assertTrue("Missing error: " + msg, containsMatch);
  }

  @Test
  public void collidingTimeformat() {
    rfa.setContext(context);
    rfa.setAppend(false);
    rfa.setPrudent(true);

    tbrp.setFileNamePattern(CoreTestConstants.OUTPUT_DIR_PREFIX + "toto-%d{dd}.log.zip");
    tbrp.start();
    rfa.setRollingPolicy(tbrp);

    rfa.start();

    StatusChecker checker = new StatusChecker(context);
    assertFalse(rfa.isStarted());
    assertEquals(Status.ERROR, checker.getHighestLevel(0));
    StatusPrinter.print(context);
    checker.assertContainsMatch("The date format in FileNamePattern will result");
  }

  @Test
  public void collidingFileNamePattern() {
    String filenamePattern = CoreTestConstants.OUTPUT_DIR_PREFIX + diff+ "-collision-%d.log.zip";
    RollingFileAppender<Object> appender0 = new RollingFileAppender<Object>();
    appender0.setName("FA0");
    appender0.setContext(context);
    appender0.setEncoder(new DummyEncoder<Object>());
    TimeBasedRollingPolicy<Object> tbrp0 = new TimeBasedRollingPolicy<Object>();
    tbrp0.setContext(context);
    tbrp0.setFileNamePattern(filenamePattern);
    tbrp0.setParent(appender0);
    tbrp0.start();
    appender0.setRollingPolicy(tbrp0);
    appender0.start();
    assertTrue(appender0.isStarted());



    RollingFileAppender<Object> appender1 = new RollingFileAppender<Object>();
    appender1.setName("FA1");
    appender1.setFile("X");
    appender1.setContext(context);
    appender1.setEncoder(new DummyEncoder<Object>());
    TimeBasedRollingPolicy<Object> tbrp1 = new TimeBasedRollingPolicy<Object>();
    tbrp1.setContext(context);
    tbrp1.setFileNamePattern(filenamePattern);
    tbrp1.setParent(appender1);
    tbrp1.start();
    appender1.setRollingPolicy(tbrp1);
    appender1.start();

    //StatusPrinter.print(context);

    assertFalse(appender1.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "'FileNamePattern' option has the same value");
  }

  /** A rolling policy (not a triggering policy) whose rollover runs a test-supplied action. */
  static class ScriptedRollingPolicy extends RollingPolicyBase {
    interface Action {
      void run() throws RolloverFailure;
    }

    Action onRollover;

    @Override
    public void rollover() throws RolloverFailure {
      onRollover.run();
    }

    @Override
    public String getActiveFileName() {
      return getParentsRawFileProperty();
    }
  }

  /** A rolling policy that is its own triggering policy and uses no file name pattern. */
  static class PatternlessRollingPolicy extends RollingPolicyBase implements TriggeringPolicy<Object> {
    @Override
    public void rollover() {
    }

    @Override
    public String getActiveFileName() {
      return getParentsRawFileProperty();
    }

    @Override
    public boolean isTriggeringEvent(File activeFile, Object event) {
      return false;
    }
  }

  private String tmpPath(String name) {
    return new File(tmp.getRoot(), name).getAbsolutePath();
  }

  private static String read(String fileName) throws IOException {
    return new String(Files.readAllBytes(new File(fileName).toPath()), "UTF-8");
  }

  private SizeBasedTriggeringPolicy<Object> startedSizeBasedTriggeringPolicy() {
    SizeBasedTriggeringPolicy<Object> policy = new SizeBasedTriggeringPolicy<Object>();
    policy.setContext(context);
    policy.start();
    return policy;
  }

  private ScriptedRollingPolicy startAppenderWithScriptedPolicy(String fileName) {
    rfa.setContext(context);
    rfa.setEncoder(new EchoEncoder<Object>());
    rfa.setFile(fileName);
    ScriptedRollingPolicy policy = new ScriptedRollingPolicy();
    policy.setContext(context);
    policy.setParent(rfa);
    policy.start();
    rfa.setRollingPolicy(policy);
    rfa.setTriggeringPolicy(startedSizeBasedTriggeringPolicy());
    rfa.start();
    assertTrue(rfa.isStarted());
    return policy;
  }

  private TimeBasedRollingPolicy<Object> startedTimeBasedRollingPolicy(RollingFileAppender<Object> appender, String pattern) {
    TimeBasedRollingPolicy<Object> policy = new TimeBasedRollingPolicy<Object>();
    policy.setContext(context);
    policy.setFileNamePattern(pattern);
    policy.setParent(appender);
    policy.start();
    return policy;
  }

  @Test
  public void startWithoutTriggeringPolicyIsRefused() {
    rfa.setContext(context);
    rfa.setFile(tmpPath("no-tp.log"));

    rfa.start();

    assertFalse(rfa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.WARN, "No TriggeringPolicy was set for the RollingFileAppender named test");
    checker.assertContainsMatch(Status.WARN, "For more information, please visit .*#rfa_no_tp");
    assertFalse(new File(tmpPath("no-tp.log")).exists());
  }

  @Test
  public void startWithoutRollingPolicyIsRefused() {
    rfa.setContext(context);
    rfa.setFile(tmpPath("no-rp.log"));
    rfa.setTriggeringPolicy(startedSizeBasedTriggeringPolicy());

    rfa.start();

    assertFalse(rfa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "No RollingPolicy was set for the RollingFileAppender named test");
    checker.assertContainsMatch(Status.ERROR, "For more information, please visit .*#rfa_no_rp");
    assertFalse(new File(tmpPath("no-rp.log")).exists());
  }

  @Test
  public void stopWithoutRollingPolicyStillStopsTriggeringPolicy() {
    rfa.setContext(context);
    SizeBasedTriggeringPolicy<Object> triggeringPolicy = startedSizeBasedTriggeringPolicy();
    rfa.setTriggeringPolicy(triggeringPolicy);

    rfa.stop();

    assertNull(rfa.getRollingPolicy());
    assertFalse(triggeringPolicy.isStarted());
  }

  @Test
  public void stopWithoutTriggeringPolicyStillStopsRollingPolicy() {
    rfa.setContext(context);
    rfa.setFile(tmpPath("fw.log"));
    FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
    rollingPolicy.setContext(context);
    rollingPolicy.setFileNamePattern(tmpPath("fw.%i.log"));
    rollingPolicy.setParent(rfa);
    rollingPolicy.start();
    rfa.setRollingPolicy(rollingPolicy);

    rfa.stop();

    assertNull(rfa.getTriggeringPolicy());
    assertFalse(rollingPolicy.isStarted());
  }

  @Test
  public void setTriggeringPolicyThatIsAlsoRollingPolicySetsBoth() {
    rfa.setTriggeringPolicy(tbrp);

    assertSame(tbrp, rfa.getTriggeringPolicy());
    assertSame(tbrp, rfa.getRollingPolicy());
  }

  @Test
  public void setRollingPolicyThatIsNotTriggeringPolicyLeavesTriggeringPolicyUnset() {
    FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();

    rfa.setRollingPolicy(rollingPolicy);

    assertSame(rollingPolicy, rfa.getRollingPolicy());
    assertNull(rfa.getTriggeringPolicy());
  }

  @Test
  public void testFilePropertyAfterNonTriggeringRollingPolicy() {
    rfa.setContext(context);
    rfa.setRollingPolicy(new FixedWindowRollingPolicy());
    rfa.setFile("x");
    StatusChecker statusChecker = new StatusChecker(context.getStatusManager());
    statusChecker.assertContainsMatch(Status.ERROR,
            "File property must be set before any triggeringPolicy ");
  }

  @Test
  public void appendersWithDistinctFileNamePatternsDoNotCollide() {
    RollingFileAppender<Object> appender0 = new RollingFileAppender<Object>();
    appender0.setName("FA0");
    appender0.setContext(context);
    appender0.setEncoder(new DummyEncoder<Object>());
    appender0.setRollingPolicy(startedTimeBasedRollingPolicy(appender0, tmpPath("a-%d.log")));
    appender0.start();

    rfa.setContext(context);
    rfa.setRollingPolicy(startedTimeBasedRollingPolicy(rfa, tmpPath("b-%d.log")));
    rfa.start();

    assertTrue(appender0.isStarted());
    assertTrue(rfa.isStarted());
    new StatusChecker(context).assertNoMatch("'FileNamePattern' option has the same value");
    appender0.stop();
    rfa.stop();
  }

  @Test
  public void startsWithoutCollisionCheckWhenContextHasNoFileNamePatternMap() {
    context.putObject(CoreConstants.RFA_FILENAME_PATTERN_COLLISION_MAP, null);
    rfa.setContext(context);
    rfa.setRollingPolicy(startedTimeBasedRollingPolicy(rfa, tmpPath("c-%d.log")));

    rfa.start();
    assertTrue(rfa.isStarted());

    rfa.stop();
    assertFalse(rfa.isStarted());
  }

  @Test
  public void stoppingUnnamedAppenderKeepsOtherAppendersInCollisionMap() {
    @SuppressWarnings("unchecked")
    Map<String, FileNamePattern> map =
        (Map<String, FileNamePattern>) context.getObject(CoreConstants.RFA_FILENAME_PATTERN_COLLISION_MAP);
    FileNamePattern otherPattern = new FileNamePattern(tmpPath("other-%d.log"), context);
    map.put("other", otherPattern);
    rfa.setName(null);
    rfa.setContext(context);
    rfa.setRollingPolicy(startedTimeBasedRollingPolicy(rfa, tmpPath("d-%d.log")));
    rfa.start();
    assertTrue(rfa.isStarted());

    rfa.stop();

    assertEquals(1, map.size());
    assertSame(otherPattern, map.get("other"));
  }

  @Test
  public void rollingPolicyWithoutFileNamePatternSkipsPatternChecks() throws IOException {
    rfa.setContext(context);
    rfa.setEncoder(new EchoEncoder<Object>());
    rfa.setFile(tmpPath("patternless.log"));
    PatternlessRollingPolicy policy = new PatternlessRollingPolicy();
    policy.setContext(context);
    policy.setParent(rfa);
    policy.start();
    rfa.setRollingPolicy(policy);

    rfa.start();
    rfa.doAppend("hello");
    rfa.stop();

    assertSame(policy, rfa.getTriggeringPolicy());
    assertEquals("hello" + CoreConstants.LINE_SEPARATOR, read(tmpPath("patternless.log")));
  }

  @Test
  public void failedRolloverKeepsAppendingToActiveFile() throws IOException {
    String fileName = tmpPath("deferred.log");
    ScriptedRollingPolicy policy = startAppenderWithScriptedPolicy(fileName);
    policy.onRollover = new ScriptedRollingPolicy.Action() {
      @Override
      public void run() throws RolloverFailure {
        throw new RolloverFailure("cannot rename");
      }
    };
    rfa.doAppend("before");
    rfa.setAppend(false);

    rfa.rollover();
    rfa.doAppend("after");
    rfa.stop();

    assertTrue(rfa.isAppend());
    new StatusChecker(context).assertContainsMatch(Status.WARN, "RolloverFailure occurred. Deferring roll-over.");
    assertEquals("before" + CoreConstants.LINE_SEPARATOR + "after" + CoreConstants.LINE_SEPARATOR, read(fileName));
  }

  @Test
  public void failureToReopenActiveFileAfterRolloverIsReported() {
    final File activeFile = new File(tmpPath("reopen.log"));
    ScriptedRollingPolicy policy = startAppenderWithScriptedPolicy(activeFile.getAbsolutePath());
    policy.onRollover = new ScriptedRollingPolicy.Action() {
      @Override
      public void run() {
        // leave something that cannot be opened for writing where the active file was
        assertTrue(activeFile.delete());
        assertTrue(activeFile.mkdir());
      }
    };

    rfa.rollover();

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    Status last = statuses.get(statuses.size() - 1);
    assertEquals(Status.ERROR, last.getLevel());
    assertEquals("setFile(" + activeFile.getAbsolutePath() + ", false) call failed.", last.getMessage());
    assertTrue(String.valueOf(last.getThrowable()), last.getThrowable() instanceof FileNotFoundException);
    assertEquals(activeFile, rfa.currentlyActiveFile);
    rfa.stop();
  }

  // 2024-01-01T12:00:00Z
  private static final long JAN_1_NOON_GMT = 1704110400000L;

  private TimeBasedRollingPolicy<Object> startedTimeBasedRollingPolicyAt(RollingFileAppender<Object> appender,
                                                                        String pattern, long currentTime) {
    TimeBasedRollingPolicy<Object> policy = new TimeBasedRollingPolicy<Object>();
    policy.setContext(context);
    policy.setFileNamePattern(pattern);
    policy.setParent(appender);
    policy.timeBasedFileNamingAndTriggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<Object>();
    policy.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
    policy.start();
    return policy;
  }

  @Test
  public void prudentModeKeepsEveryEventLoggedConcurrently() throws Exception {
    final int threadCount = 2;
    final int eventsPerThread = 200;
    // the threads' first events are encoded at the same time, i.e. each while
    // the other thread's append is in progress: see
    // FileAppenderTest.prudentAppenderKeepsEveryEventLoggedConcurrently
    final RollingFileAppender<Object> prudentRfa = new RollingFileAppender<Object>();
    prudentRfa.setContext(context);
    prudentRfa.setName("prudent-concurrent");
    prudentRfa.setEncoder(new RendezvousEncoder(threadCount));
    prudentRfa.setPrudent(true);
    prudentRfa.setRollingPolicy(startedTimeBasedRollingPolicyAt(prudentRfa,
        tmpPath("concurrent-%d{yyyy-MM-dd, GMT}.log"), JAN_1_NOON_GMT));
    prudentRfa.start();
    assertTrue(prudentRfa.isStarted());

    final CountDownLatch startGate = new CountDownLatch(1);
    final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());
    List<Thread> threads = new ArrayList<Thread>();
    List<String> expected = new ArrayList<String>();
    for (int t = 0; t < threadCount; t++) {
      final String prefix = "thread" + t + "-event";
      for (int i = 0; i < eventsPerThread; i++) {
        expected.add(prefix + i);
      }
      Thread thread = new Thread(() -> {
        try {
          startGate.await();
          for (int i = 0; i < eventsPerThread; i++) {
            prudentRfa.doAppend(prefix + i);
          }
        } catch (Throwable e) {
          failures.add(e);
        }
      }, "prudent-rolling-writer-" + t);
      thread.start();
      threads.add(thread);
    }

    startGate.countDown();
    for (Thread thread : threads) {
      thread.join(TimeUnit.SECONDS.toMillis(30));
      assertFalse(thread.getName() + " did not finish", thread.isAlive());
    }
    prudentRfa.stop();

    assertEquals(Collections.<Throwable>emptyList(), failures);
    List<String> lines = new ArrayList<String>(Arrays.asList(
        read(tmpPath("concurrent-2024-01-01.log")).split(CoreConstants.LINE_SEPARATOR)));
    assertEquals("events in the file", expected.size(), lines.size());
    Collections.sort(expected);
    Collections.sort(lines);
    assertEquals(expected, lines);
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentModeLocksTheFileItRollsOverTo() throws IOException {
    final List<LockProbingOutputStream> opened = new ArrayList<LockProbingOutputStream>();
    RollingFileAppender<Object> prudentRfa = new RollingFileAppender<Object>() {
      // writes each file it opens through a LockProbingOutputStream
      @Override
      protected boolean openFile(String filename) throws IOException {
        boolean result = super.openFile(filename);
        LockProbingOutputStream probe =
            new LockProbingOutputStream(((ResilientFileOutputStream) getOutputStream()).getFile());
        probe.setContext(getContext());
        opened.add(probe);
        setOutputStream(probe);
        return result;
      }
    };
    prudentRfa.setContext(context);
    prudentRfa.setName("prudent-rolling");
    prudentRfa.setEncoder(new EchoEncoder<Object>());
    prudentRfa.setPrudent(true);
    TimeBasedRollingPolicy<Object> policy = startedTimeBasedRollingPolicyAt(prudentRfa,
        tmpPath("prudent-%d{yyyy-MM-dd, GMT}.log"), JAN_1_NOON_GMT);
    prudentRfa.setRollingPolicy(policy);
    prudentRfa.start();

    prudentRfa.doAppend("day 1");
    policy.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(JAN_1_NOON_GMT + TimeUnit.DAYS.toMillis(1));
    prudentRfa.doAppend("day 2");
    prudentRfa.stop();

    assertEquals("day 1" + CoreConstants.LINE_SEPARATOR, read(tmpPath("prudent-2024-01-01.log")));
    assertEquals("day 2" + CoreConstants.LINE_SEPARATOR, read(tmpPath("prudent-2024-01-02.log")));
    // the file opened at start and the one opened by the rollover were both
    // written under their lock
    assertEquals(2, opened.size());
    assertEquals(Collections.singletonList(Boolean.TRUE), opened.get(0).getLockHeldDuringWrite());
    assertEquals(Collections.singletonList(Boolean.TRUE), opened.get(1).getLockHeldDuringWrite());
    new StatusChecker(context).assertIsErrorFree();
  }

  /**
   * Holds each event in encode() until as many events as there are parties
   * are being encoded at the same time, so that the first events of that many
   * threads are written concurrently; later events are not held. The wait is
   * bounded, and the events are encoded when it ends either way, so the
   * outcome does not depend on how long it takes.
   */
  static class RendezvousEncoder extends EchoEncoder<Object> {
    final CountDownLatch encoding;

    RendezvousEncoder(int parties) {
      encoding = new CountDownLatch(parties);
    }

    @Override
    public byte[] encode(Object event) {
      encoding.countDown();
      try {
        encoding.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return super.encode(event);
    }
  }
}
