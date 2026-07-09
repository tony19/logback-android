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

import ch.qos.logback.core.pattern.SpacePadder;
import ch.qos.logback.core.util.FileSize;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static ch.qos.logback.core.CoreConstants.DAILY_DATE_PATTERN;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeBasedRollingWithArchiveRemoval_Test extends ScaffoldingForRollingTests {
  static long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;

  private RollingFileAppender<Object> rfa;
  private TimeBasedRollingPolicy<Object> tbrp;
  private TimeBasedFileNamingAndTriggeringPolicy<Object> tbfnatp;
  private ConfigParameters cp;

  @Before
  @Override
  public void setUp() throws ParseException {
    super.setUp();
    this.rfa = new RollingFileAppender<Object>();
    this.tbrp = new TimeBasedRollingPolicy<Object>();
    this.tbfnatp = new DefaultTimeBasedFileNamingAndTriggeringPolicy<Object>();
    this.cp = new ConfigParameters(currentTime);
  }

  @Test
  public void monthlyRolloverOverManyPeriods() {
    final String fileNamePattern = randomOutputDir + "/%d{yyyy/MM, GMT}/clean.txt.zip";
    final long MILLIS_IN_MONTH = (long) ((365.242199 / 12) * MILLIS_IN_DAY);
    cp.maxHistory(2).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(30).periodDurationInMillis(MILLIS_IN_MONTH);

    logOverMultiplePeriods(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder("2020", "2021"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020").list()), containsInAnyOrder("11", "12"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020/11").list()), contains("clean.txt.zip"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020/12").list()), contains("clean.txt.zip"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2021/01").list()), contains("clean.txt"));
  }

  private long generateDailyRollover(ConfigParameters cp) {
    cp.fileNamePattern(randomOutputDir + "clean-%d{" + DAILY_DATE_PATTERN + ", GMT}.txt");
    return logOverMultiplePeriods(cp);
  }

  @Test
  public void checkCleanupForBasicDailyRollover() {
    cp.maxHistory(6).simulatedNumberOfPeriods(30).startInactivity(10).numInactivityPeriods(1);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-08-04.txt",
      "clean-2018-08-06.txt",
      "clean-2018-08-07.txt",
      "clean-2018-08-08.txt",
      "clean-2018-08-09.txt",
      "clean-2018-08-10.txt",
      "clean-2018-08-05.txt"));
  }

  @Test
  public void checkCleanupForBasicDailyRolloverWithSizeCap() {
    long bytesOutputPerPeriod = 15984;
    int sizeInUnitsOfBytesPerPeriod = 2;

    cp.maxHistory(5).simulatedNumberOfPeriods(10).sizeCap(sizeInUnitsOfBytesPerPeriod * bytesOutputPerPeriod+1000);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-07-19.txt",
      "clean-2018-07-20.txt",
      "clean-2018-07-21.txt"));
  }

  @Test
  public void checkCleanupForBasicDailyRolloverWithMaxSize() {
    cp.maxHistory(6).simulatedNumberOfPeriods(70).startInactivity(30).numInactivityPeriods(1);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-09-13.txt",
      "clean-2018-09-14.txt",
      "clean-2018-09-15.txt",
      "clean-2018-09-16.txt",
      "clean-2018-09-17.txt",
      "clean-2018-09-18.txt",
      "clean-2018-09-19.txt"));
  }

  // Since the duration of a month (in seconds) varies from month to month, tests with inactivity period must
  // be conducted with daily rollover  not monthly
  @Test
  public void checkCleanupForDailyRollover_15Periods() {
    cp.maxHistory(5).simulatedNumberOfPeriods(15).startInactivity(6).numInactivityPeriods(3);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-07-21.txt",
      "clean-2018-07-22.txt",
      "clean-2018-07-23.txt",
      "clean-2018-07-24.txt",
      "clean-2018-07-25.txt",
      "clean-2018-07-26.txt"));
  }

  @Test
  public void checkCleanupForDailyRolloverWithInactivity_30Periods() {
    cp.maxHistory(2).simulatedNumberOfPeriods(30).startInactivity(3).numInactivityPeriods(1);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-08-08.txt",
      "clean-2018-08-09.txt",
      "clean-2018-08-10.txt"));
  }

  @Test
  public void checkCleanupForDailyRolloverWithInactivity_10Periods() {
    cp.maxHistory(6).simulatedNumberOfPeriods(10).startInactivity(2).numInactivityPeriods(2);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-07-15.txt",
      "clean-2018-07-16.txt",
      "clean-2018-07-17.txt",
      "clean-2018-07-18.txt",
      "clean-2018-07-19.txt",
      "clean-2018-07-20.txt",
      "clean-2018-07-21.txt"));
  }

  @Test
  public void checkCleanupForDailyRolloverWithSecondPhase() {
    final int maxHistory = 5;
    final String fileNamePattern = randomOutputDir + "clean-%d{" + DAILY_DATE_PATTERN + ", GMT}.txt";

    ConfigParameters cp0 = new ConfigParameters(currentTime).maxHistory(maxHistory).fileNamePattern(fileNamePattern)
            .simulatedNumberOfPeriods(maxHistory * 2);
    final long endTime = logOverMultiplePeriods(cp0);

    ConfigParameters cp1 = new ConfigParameters(endTime + MILLIS_IN_DAY * 10).maxHistory(maxHistory).fileNamePattern(fileNamePattern)
            .simulatedNumberOfPeriods(maxHistory);
    logOverMultiplePeriods(cp1);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-07-31.txt",
      "clean-2018-08-01.txt",
      "clean-2018-08-02.txt",
      "clean-2018-08-03.txt",
      "clean-2018-08-04.txt",
      "clean-2018-08-05.txt"));
  }

  @Test
  public void dailySizeBasedRolloverWithoutCap() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    tbfnatp = sizeAndTimeBasedFNATP;
    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + ", GMT}-clean.%i.zip";
    cp.maxHistory(5).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(5 * 4);
    logOverMultiplePeriods(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "2018-07-26-clean.0.zip",
      "2018-07-26-clean.1.zip",
      "2018-07-27-clean.0.zip",
      "2018-07-27-clean.1.zip",
      "2018-07-28-clean.0.zip",
      "2018-07-28-clean.1.zip",
      "2018-07-29-clean.0.zip",
      "2018-07-29-clean.1.zip",
      "2018-07-30-clean.0.zip",
      "2018-07-30-clean.1.zip",
      "2018-07-31-clean.0"));
  }

  @Test
  public void dailyChronologSizeBasedRollover() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    tbfnatp = sizeAndTimeBasedFNATP;
    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + ", GMT}/clean.%i.zip";
    cp.maxHistory(5).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(5 * 3);
    logOverMultiplePeriods(cp);

    final String[] dirNames = new String[] {
      "2018-07-21",
      "2018-07-22",
      "2018-07-23",
      "2018-07-24",
      "2018-07-25",
      "2018-07-26"
    };
    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(dirNames));

    // the last dir contains the active file
    List<String> dirs = Arrays.asList(dirNames).subList(0, dirNames.length - 1);
    for (String dir : dirs) {
      assertThat(Arrays.asList(new File(ROOT_DIR, dir).list()), containsInAnyOrder("clean.0.zip", "clean.1.zip"));
    }
    assertThat(Arrays.asList(new File(ROOT_DIR, "2018-07-26").list()), contains("clean.0"));
  }

  @Test
  public void dailyChronologSizeBasedRolloverWithSecondPhase() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    tbfnatp = sizeAndTimeBasedFNATP;

    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + ", GMT}/clean.%i";
    final int maxHistory = 5;
    cp.maxHistory(maxHistory).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(3);
    final long endTime = logOverMultiplePeriods(cp);

    ConfigParameters cp1 = new ConfigParameters(endTime + MILLIS_IN_DAY * 7).maxHistory(maxHistory).fileNamePattern(fileNamePattern)
            .simulatedNumberOfPeriods(maxHistory * 4);
    logOverMultiplePeriods(cp1);

    final String[] dirNames = new String[]{
      "2018-08-05",
      "2018-08-06",
      "2018-08-07",
      "2018-08-08",
      "2018-08-09",
      "2018-08-10"
    };
    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(dirNames));

    // the last dir contains the active file
    List<String> dirs = Arrays.asList(dirNames).subList(0, dirNames.length - 1);
    for (String dir : dirs) {
      assertThat(Arrays.asList(new File(ROOT_DIR, dir).list()), containsInAnyOrder("clean.0", "clean.1"));
    }
    assertThat(Arrays.asList(new File(ROOT_DIR, "2018-08-10").list()), contains("clean.0"));
  }

  private void buildRollingFileAppender(ConfigParameters cp) {
    rfa.setContext(context);
    rfa.setEncoder(encoder);
    tbrp.setContext(context);
    tbrp.setFileNamePattern(cp.fileNamePattern);
    tbrp.setMaxHistory(cp.maxHistory);
    tbrp.setTotalSizeCap(new FileSize(cp.sizeCap));
    tbrp.setParent(rfa);
    tbrp.setCleanHistoryOnStart(false);
    tbrp.timeBasedFileNamingAndTriggeringPolicy = tbfnatp;
    tbrp.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(cp.simulatedTime);
    tbrp.start();
    rfa.setRollingPolicy(tbrp);
    rfa.start();
  }

  private long logOverMultiplePeriods(ConfigParameters cp) {
    buildRollingFileAppender(cp);
    final int ticksPerPeriod = 216;
    int runLength = cp.simulatedNumberOfPeriods * ticksPerPeriod;
    int startInactivityIndex = cp.startInactivity * ticksPerPeriod;
    int endInactivityIndex = startInactivityIndex + cp.numInactivityPeriods * ticksPerPeriod;
    long tickDuration = cp.periodDurationInMillis / ticksPerPeriod;

    for (int i = 0; i <= runLength; i++) {
      if (i < startInactivityIndex || i > endInactivityIndex) {
        Date currentDate = new Date(tbrp.timeBasedFileNamingAndTriggeringPolicy.getCurrentTime());
        StringBuilder sb = new StringBuilder("Hello");
        String currentDateStr = currentDate.toString();
        String iAsString = Integer.toString(i);
        sb.append(currentDateStr);
        SpacePadder.spacePad(sb, 66 + (3 - iAsString.length() - currentDateStr.length()));
        sb.append(iAsString);
        rfa.doAppend(sb.toString());
      }
      tbrp.timeBasedFileNamingAndTriggeringPolicy.setCurrentTime(tbrp.timeBasedFileNamingAndTriggeringPolicy.getCurrentTime() +
              tickDuration);
      add(tbrp.compressionFuture);
      add(tbrp.cleanUpFuture);
      waitForJobsToComplete();
    }

    rfa.stop();
    return tbrp.timeBasedFileNamingAndTriggeringPolicy.getCurrentTime();
  }
}
