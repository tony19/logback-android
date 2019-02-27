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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.testUtil.EnvUtilForTests;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * A rather exhaustive set of tests. Tests include leaving the file option
 * blank, or setting it, with and without compression, and tests with or without
 * stopping/restarting the RollingFileAppender.
 * <p/>
 * The regression tests log a few times using a RollingFileAppender. Then, they
 * predict the names of the files which should be generated and compare them
 * with witness files.
 * <p/>
 * <pre>
 *                Compression     file option    Stop/Restart
 *     Test1      NO              BLANK           NO
 *     Test2      YES             BLANK           NO
 *     Test3      NO              BLANK           YES
 *     Test4      NO              SET             YES
 *     Test5      NO              SET             NO
 *     Test6      YES             SET             NO
 * </pre>
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class TimeBasedRollingTest extends ScaffoldingForRollingTests {

  private static final int NO_RESTART = 0;
  private static final int WITH_RESTART = 1;
  private static final int WITH_RESTART_AND_LONG_WAIT = 2000;

  private static final boolean FILE_OPTION_SET = true;
  private static final boolean FILE_OPTION_BLANK = false;


  private RollingFileAppender<Object> rfa1 = new RollingFileAppender<Object>();
  private TimeBasedRollingPolicy<Object> tbrp1 = new TimeBasedRollingPolicy<Object>();

  private RollingFileAppender<Object> rfa2 = new RollingFileAppender<Object>();
  private TimeBasedRollingPolicy<Object> tbrp2 = new TimeBasedRollingPolicy<Object>();

  private EchoEncoder<Object> encoder = new EchoEncoder<Object>();

  private RolloverChecker rolloverChecker;

  private void initRFA(RollingFileAppender<Object> rfa, String filename) {
    rfa.setContext(context);
    rfa.setEncoder(encoder);
    if (filename != null) {
      rfa.setFile(filename);
    }
  }

  private void initTRBP(RollingFileAppender<Object> rfa,
                TimeBasedRollingPolicy<Object> tbrp, String filenamePattern,
                long givenTime) {
    tbrp.setContext(context);
    tbrp.setFileNamePattern(filenamePattern);
    tbrp.setParent(rfa);
    tbrp.timeBasedFileNamingAndTriggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<Object>();
    tbrp.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(givenTime);
    rfa.setRollingPolicy(tbrp);
    tbrp.start();
    rfa.start();
  }

  private void genericTest(String testId, String patternPrefix, String compressionSuffix, boolean fileOptionIsSet, int waitDuration) throws IOException {
    String fileName = fileOptionIsSet ? testId2FileName(testId) : null;
    initRFA(rfa1, fileName);

    String fileNamePatternStr = randomOutputDir + patternPrefix + "-%d{" + DATE_PATTERN_WITH_SECONDS + ", GMT}" + compressionSuffix;

    initTRBP(rfa1, tbrp1, fileNamePatternStr, currentTime);

    // compute the current filename
    addExpectedFileName_ByDate(fileNamePatternStr, getMillisOfCurrentPeriodsStart());

    incCurrentTime(1100);
    tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);

    for (int i = 0; i < 3; i++) {
      rfa1.doAppend("Hello---" + i);
      addExpectedFileNamedIfItsTime_ByDate(fileNamePatternStr);
      incCurrentTime(500);
      tbrp1.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
      add(tbrp1.compressionFuture);
      add(tbrp1.cleanUpFuture);
    }
    rfa1.stop();
    waitForJobsToComplete();

    if (waitDuration != NO_RESTART) {
      doRestart(testId, patternPrefix, fileOptionIsSet, waitDuration);
    }
    waitForJobsToComplete();

    massageExpectedFilesToCorresponToCurrentTarget(fileName, fileOptionIsSet);
    StatusPrinter.print(context);
    rolloverChecker.check(expectedFilenameList);
  }

  private void defaultTest(String testId, String patternPrefix, String compressionSuffix, boolean fileOptionIsSet, int waitDuration) throws IOException {
    boolean withCompression = compressionSuffix.length() > 0;
    rolloverChecker = new DefaultRolloverChecker(testId, withCompression, compressionSuffix);
    genericTest(testId, patternPrefix, compressionSuffix, fileOptionIsSet, waitDuration);
  }

  private void doRestart(String testId, String patternPart, boolean fileOptionIsSet, int waitDuration) {
    // change the timestamp of the currently actively file
    File activeFile = new File(rfa1.getFile());
    activeFile.setLastModified(currentTime);

    incCurrentTime(waitDuration);

    String filePatternStr = randomOutputDir + patternPart + "-%d{" + DATE_PATTERN_WITH_SECONDS + ", GMT}";

    String fileName = fileOptionIsSet ? testId2FileName(testId) : null;
    initRFA(rfa2, fileName);
    initTRBP(rfa2, tbrp2, filePatternStr, currentTime);
    for (int i = 0; i < 3; i++) {
      rfa2.doAppend("World---" + i);
      addExpectedFileNamedIfItsTime_ByDate(filePatternStr);
      incCurrentTime(400);
      tbrp2.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(currentTime);
      add(tbrp2.compressionFuture);
      add(tbrp2.cleanUpFuture);
    }
    rfa2.stop();
  }


  @Test
  public void noCompression_FileBlank_NoRestart_1() throws IOException {
    defaultTest("test1", "test1", "", FILE_OPTION_BLANK, NO_RESTART);
  }

  @Test
  public void withCompression_FileBlank_NoRestart_2() throws IOException {
    defaultTest("test2", "test2", ".gz", FILE_OPTION_BLANK, NO_RESTART);
  }

  @Test
  public void noCompression_FileBlank_StopRestart_3() throws IOException {
    defaultTest("test3", "test3", "", FILE_OPTION_BLANK, WITH_RESTART);
  }

  @Test
  public void noCompression_FileSet_StopRestart_4() throws IOException {
    defaultTest("test4", "test4", "", FILE_OPTION_SET, WITH_RESTART);
  }

  @Test
  public void noCompression_FileSet_StopRestart_WithLongWait_4B() throws IOException {
    defaultTest("test4B", "test4B", "", FILE_OPTION_SET, WITH_RESTART_AND_LONG_WAIT);
  }

  @Test
  public void noCompression_FileSet_NoRestart_5() throws IOException {
    defaultTest("test5", "test5", "", FILE_OPTION_SET, NO_RESTART);
  }

  @Test
  public void withCompression_FileSet_NoRestart_6() throws IOException {
    defaultTest("test6", "test6", ".gz", FILE_OPTION_SET, NO_RESTART);
  }

  // LOGBACK-168
  @Test
  public void withMissingTargetDirWithCompression() throws IOException {
    defaultTest("test8", "%d{yyyy-MM-dd, aux}/test8", ".zip", FILE_OPTION_SET, NO_RESTART);
  }

  @Test
  public void withMissingTargetDirWithZipCompression() throws IOException {
    defaultTest("test8", "%d{yyyy-MM-dd, aux}/", ".zip", FILE_OPTION_SET, NO_RESTART);
  }

  @Test
  public void failed_rename() throws IOException {
    if (!EnvUtilForTests.isWindows())
      return;

    FileOutputStream fos = null;
    try {
      String fileName = testId2FileName("failed_rename");
      File file = new File(fileName);
      file.getParentFile().mkdirs();

      fos = new FileOutputStream(fileName);

      String testId = "failed_rename";
      rolloverChecker = new ZRolloverChecker(testId);
      genericTest(testId, "failed_rename", "", FILE_OPTION_SET, NO_RESTART);


    } finally {
      StatusPrinter.print(context);
      if (fos != null) fos.close();
    }
  }

}
