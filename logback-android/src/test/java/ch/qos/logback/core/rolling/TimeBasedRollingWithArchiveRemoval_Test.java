/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 * or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.rolling;

import ch.qos.logback.core.pattern.SpacePadder;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.FixedRateInvocationGate;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import static ch.qos.logback.core.CoreConstants.DAILY_DATE_PATTERN;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeBasedRollingWithArchiveRemoval_Test extends ScaffoldingForRollingTests {

  private RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  private TimeBasedRollingPolicy<Object> tbrp = new TimeBasedRollingPolicy<Object>();
  private TimeBasedFileNamingAndTriggeringPolicy<Object> tbfnatp = new DefaultTimeBasedFileNamingAndTriggeringPolicy<Object>();

  static long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
  private static long MILLIS_IN_MONTH = (long) ((365.242199 / 12) * MILLIS_IN_DAY);

  private int ticksPerPeriod = 216;
  private ConfigParameters cp; // initialized in setup

  private FixedRateInvocationGate fixedRateInvocationGate = new FixedRateInvocationGate(ticksPerPeriod/2);

  @Before
  @Override
  public void setUp() throws ParseException {
    super.setUp();
    this.cp = new ConfigParameters(currentTime);
  }

  @Test
  public void monthlyRolloverOverManyPeriods() {
    final String fileNamePattern = randomOutputDir + "/%d{yyyy/MM}/clean.txt.zip";
    cp.maxHistory(2).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(30).periodDurationInMillis(MILLIS_IN_MONTH);

    logOverMultiplePeriods(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), contains("2020"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020").list()), contains("11", "12"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020/11").list()), contains("clean.txt.zip"));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2020/12").list()), contains("clean.txt"));
  }

  private long generateDailyRollover(ConfigParameters cp) {
    cp.fileNamePattern(randomOutputDir + "clean-%d{" + DAILY_DATE_PATTERN + "}.txt");
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
      "clean-2018-07-18.txt",
      "clean-2018-07-19.txt",
      "clean-2018-07-20.txt"));
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
      "clean-2018-09-18.txt"));
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
      "clean-2018-07-25.txt"));
  }

  @Test
  public void checkCleanupForDailyRolloverWithInactivity_30Periods() {
    cp.maxHistory(2).simulatedNumberOfPeriods(30).startInactivity(3).numInactivityPeriods(1);
    generateDailyRollover(cp);

    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(
      "clean-2018-08-08.txt",
      "clean-2018-08-09.txt"));
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
      "clean-2018-07-20.txt"));
  }

  @Test
  public void checkCleanupForDailyRolloverWithSecondPhase() {
    final int maxHistory = 5;
    final String fileNamePattern = randomOutputDir + "clean-%d{" + DAILY_DATE_PATTERN + "}.txt";

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
      "clean-2018-08-04.txt"));
  }

  @Test
  public void dailySizeBasedRolloverWithoutCap() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.invocationGate = fixedRateInvocationGate;
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    tbfnatp = sizeAndTimeBasedFNATP;
    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + "}-clean.%i.zip";
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
      "2018-07-30-clean.0"));
  }

  @Test
  public void dailyChronologSizeBasedRollover() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    sizeAndTimeBasedFNATP.invocationGate = fixedRateInvocationGate;
    tbfnatp = sizeAndTimeBasedFNATP;
    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + "}/clean.%i.zip";
    cp.maxHistory(5).fileNamePattern(fileNamePattern).simulatedNumberOfPeriods(5 * 3);
    logOverMultiplePeriods(cp);

    final String[] dirNames = new String[] {
      "2018-07-21",
      "2018-07-22",
      "2018-07-23",
      "2018-07-24",
      "2018-07-25"
    };
    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(dirNames));

    Stream.of(dirNames)
      .limit(dirNames.length - 1) // the last dir contains the active file (not zipped)
      .forEach(dir -> assertThat(Arrays.asList(new File(ROOT_DIR, dir).list()), containsInAnyOrder("clean.0.zip", "clean.1.zip")));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2018-07-25").list()), contains("clean.0"));
  }

  @Test
  public void dailyChronologSizeBasedRolloverWithSecondPhase() {
    SizeAndTimeBasedFNATP<Object> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP<Object>();
    sizeAndTimeBasedFNATP.setMaxFileSize(new FileSize(10000));
    sizeAndTimeBasedFNATP.invocationGate = fixedRateInvocationGate;
    tbfnatp = sizeAndTimeBasedFNATP;

    final String fileNamePattern = randomOutputDir + "/%d{" + DAILY_DATE_PATTERN + "}/clean.%i";
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
    };
    final File ROOT_DIR = new File(randomOutputDir);
    assertThat(Arrays.asList(ROOT_DIR.list()), containsInAnyOrder(dirNames));
    Stream.of(dirNames)
      .limit(dirNames.length - 1) // the last dir contains the active file
      .forEach(dir -> assertThat(Arrays.asList(new File(ROOT_DIR, dir).list()), containsInAnyOrder("clean.0", "clean.1")));
    assertThat(Arrays.asList(new File(ROOT_DIR, "2018-08-09").list()), contains("clean.0"));
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
