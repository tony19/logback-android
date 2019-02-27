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
package ch.qos.logback.core.rolling.helper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ch.qos.logback.classic.LoggerContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TimeBasedArchiveRemoverTest {

  private final String TIMEZONE_NAME = "GMT";
  private final String DATE_FORMAT = "yyyyMMdd";
  private final Date EXPIRY = parseDate(DATE_FORMAT, "20191104");
  private final String FILENAME_PATTERN = "%d{yyyy/MM," + TIMEZONE_NAME + ", aux}/app_%d{" + DATE_FORMAT + ", " + TIMEZONE_NAME + "}.log";
  private final int MAX_HISTORY = 4;
  private final int NUM_FILES_TO_KEEP = 3;
  private File[] expiredFiles;
  private File[] recentFiles;
  private TimeBasedArchiveRemover remover;
  private FileProvider fileProvider;

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void baseSetup() throws IOException {
    setupTmpDir(tmpDir);
    fileProvider = mockFileProvider();
    remover = mockArchiveRemover(tmpDir.getRoot().getAbsolutePath() + File.separator + FILENAME_PATTERN, fileProvider);
  }

  @Test
  public void removesOnlyExpiredFiles() {
    remover.clean(EXPIRY);
    for (File f : expiredFiles) {
      verify(fileProvider).deleteFile(f);
    }
    for (File f : recentFiles) {
      verify(fileProvider, never()).deleteFile(f);
    }
  }

  @Test
  public void removesOnlyExpiredFilesOlderThanMaxHistory() {
    final int MAX_HISTORY = 2;
    remover.setMaxHistory(MAX_HISTORY);
    remover.clean(EXPIRY);

    for (File f : Arrays.asList(expiredFiles).subList(MAX_HISTORY, expiredFiles.length)) {
      verify(fileProvider).deleteFile(f);
    }
    for (File f : Arrays.asList(expiredFiles).subList(0, MAX_HISTORY)) {
      verify(fileProvider, never()).deleteFile(f);
    }
  }

  @Test
  public void removesParentDirWhenEmpty() throws IOException {
    File[] emptyDirs = new File[] {
      tmpDir.newFolder("empty_2018", "08"),
      tmpDir.newFolder("empty_2018", "12"),
      tmpDir.newFolder("empty_2019", "01"),
    };
    for (File d : emptyDirs) {
      d.deleteOnExit();
    }

    remover = mockArchiveRemover(tmpDir.getRoot().getAbsolutePath() + File.separator + "empty_%d{yyyy/MM}" + File.separator + "%d.log", fileProvider);
    remover.clean(EXPIRY);

    for (File d : emptyDirs) {
      verify(fileProvider).deleteFile(d);
    }
  }

  @Test
  public void keepsParentDirWhenNonEmpty() {
    // Setting an expiration date of 0 would cause no files to be deleted
    remover.clean(new Date(0));

    verify(fileProvider, never()).deleteFile(any(File.class));
  }

  @Test
  public void removesOlderFilesThatExceedTotalSizeCap() {
    setupSizeCapTest();
    remover.clean(EXPIRY);
    for (File f : Arrays.asList(expiredFiles).subList(MAX_HISTORY - NUM_FILES_TO_KEEP, expiredFiles.length)) {
      verify(fileProvider).deleteFile(f);
    }
  }

  @Test
  public void keepsRecentFilesAndOlderFilesWithinTotalSizeCap() {
    setupSizeCapTest();
    remover.clean(EXPIRY);

    for (File f : recentFiles) {
      verify(fileProvider, never()).deleteFile(f);
    }
    for (File f : Arrays.asList(expiredFiles).subList(0, MAX_HISTORY - NUM_FILES_TO_KEEP)) {
      verify(fileProvider, never()).deleteFile(f);
    }
  }

  private void setupSizeCapTest() {
    final long FILE_SIZE = 1024L;
// XXX: Need to use doReturn().when() here to avoid NPE
//      when(fileProvider.length(any(File.class))).thenReturn(FILE_SIZE);
//      when(fileProvider.deleteFile(any(File.class))).thenReturn(true);
    doReturn(FILE_SIZE).when(fileProvider).length(any(File.class));
    doReturn(true).when(fileProvider).deleteFile(any(File.class));
    remover.setTotalSizeCap(NUM_FILES_TO_KEEP * FILE_SIZE);
    remover.setMaxHistory(MAX_HISTORY);
  }

  private void setupTmpDir(TemporaryFolder tmpDir) throws IOException {
    File[] dirs = new File[] {
      tmpDir.newFolder("2016", "02"),
      tmpDir.newFolder("2017", "12"),
      tmpDir.newFolder("2018", "03"),
      tmpDir.newFolder("2019", "11"),
      tmpDir.newFolder("2019", "10"),
    };
    recentFiles = new File[] {
      tmpDir.newFile("2019/11/app_20191105.log"),
      tmpDir.newFile("2019/11/app_20191104.log"),
    };
    expiredFiles = new File[] {
      tmpDir.newFile("2019/11/app_20191103.log"),
      tmpDir.newFile("2019/11/app_20191102.log"),
      tmpDir.newFile("2019/10/app_20191001.log"),
      tmpDir.newFile("2018/03/app_20180317.log"),
      tmpDir.newFile("2017/12/app_20171225.log"),
      tmpDir.newFile("2016/02/app_20160214.log"),
    };

    for (File d : dirs) {
      d.deleteOnExit();
    }
    for (File f : recentFiles) {
      f.deleteOnExit();
    }
    for (File f : expiredFiles) {
      f.deleteOnExit();
    }
  }

  private Date parseDate(String format, String value) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE_NAME));
    Date date;
    try {
      date = dateFormat.parse(value);
    } catch (ParseException e) {
      date = null;
    }
    assertNotNull(date);
    return date;
  }

  private TimeBasedArchiveRemover mockArchiveRemover(String filenamePattern, FileProvider fileProvider) {
    LoggerContext context = new LoggerContext();
    RollingCalendar rollingCalendar = new RollingCalendar(DATE_FORMAT);
    FileNamePattern filePattern = new FileNamePattern(filenamePattern, context);
    TimeBasedArchiveRemover archiveRemover = new TimeBasedArchiveRemover(filePattern, rollingCalendar, fileProvider);
    archiveRemover.setContext(context);
    return spy(archiveRemover);
  }

  private FileProvider mockFileProvider() {
    return spy(new DefaultFileProvider());
  }
}
