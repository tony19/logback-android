/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 * <p>
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 * <p>
 * or (per the licensee's choosing)
 * <p>
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SQLiteAppenderTest {

  private static final String TEST_SQLITE_FILENAME = CoreTestConstants.OUTPUT_DIR_PREFIX + "SQLiteAppenderTest/logback.db";
  private static final long EXPIRY_MS = 500;
  private static final long NO_EXPIRY = 0;

  private static final String SQLITE_APPENDER_WITHOUT_MAX_HISTORY_XML =
      "<configuration>" +
          "<appender name='db' class='ch.qos.logback.classic.android.SQLiteAppender'>" +
            "<filename>" + TEST_SQLITE_FILENAME + "</filename>" +
          "</appender>" +
          "<root level='DEBUG'>" +
          "<appender-ref ref='db' />" +
          "</root>" +
          "</configuration>";

  private static final String SQLITE_APPENDER_WITH_MAX_HISTORY_XML =
      "<configuration>" +
        "<appender name='db' class='ch.qos.logback.classic.android.SQLiteAppender'>" +
          "<filename>" + TEST_SQLITE_FILENAME + "</filename>" +
          "<maxHistory>500 milli</maxHistory>" +
        "</appender>" +
        "<root level='DEBUG'>" +
          "<appender-ref ref='db' />" +
        "</root>" +
      "</configuration>";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private SQLiteLogCleaner logCleaner;
  private long startTimeMs;
  private LoggerContext context;
  private SQLiteAppender appender;

  @Before
  public void setup() throws Exception {
    context = new LoggerContext();
    context.putProperty(CoreConstants.PACKAGE_NAME_KEY, "com.example");
    appender = new SQLiteAppender();
    appender.setFilename(TEST_SQLITE_FILENAME);
    appender.setContext(context);
    logCleaner = mock(SQLiteLogCleaner.class);
  }

  @After
  public void teardown() {
    new File(TEST_SQLITE_FILENAME).delete();
  }

  @Test
  public void maxHistoryRemovesExpiredLogs() throws InterruptedException, JoranException {
    configureLogbackByString(SQLITE_APPENDER_WITH_MAX_HISTORY_XML);
    final int count = 12;
    addLogEvents(count, EXPIRY_MS / 2);

    Cursor c = getCursor();
    assertRowCount(c, 0, count);

    final int colIndex = c.getColumnIndex("timestmp");
    while (c.moveToNext()) {
      final long timestamp = c.getLong(colIndex);
      assertThat(timestamp, is(greaterThan(startTimeMs - EXPIRY_MS)));
    }
  }

  @Test
  public void maxHistoryIsDisabledByDefault() throws InterruptedException, JoranException {
    configureLogbackByString(SQLITE_APPENDER_WITHOUT_MAX_HISTORY_XML);
    final int count = 1000;
    addLogEvents(count, NO_EXPIRY);

    assertThat(getCursor().getCount(), is(count));
  }

  @Test
  public void cleanuOccursAtAppenderStartup() throws InterruptedException {
    addAppenderToContext("1 hour");
    verify(logCleaner, times(1)).performLogCleanup(any(SQLiteDatabase.class), any(Duration.class));
  }

  @Test
  public void cleanupDoesNotOccurBeforeExpiration() throws InterruptedException {
    addAppenderToContext("1 hour");

    addLogEvents(3, NO_EXPIRY);

    // log-cleanup normally called between logging events if expiry time
    // exceeded, but no expiration here, so call-count should still be 1
    verify(logCleaner, times(1)).performLogCleanup(any(SQLiteDatabase.class), any(Duration.class));
  }

  @Test
  public void cleanupOccursAfterEveryExpiration() throws InterruptedException {
    addAppenderToContext(EXPIRY_MS + " milli");

    final int count = 7;
    final long delayMs = EXPIRY_MS / 2;
    final int expectedCallCount = (int)Math.ceil((double)(delayMs * count)/EXPIRY_MS);
    addLogEvents(count, delayMs);

    verify(logCleaner, times(expectedCallCount)).performLogCleanup(any(SQLiteDatabase.class), any(Duration.class));
  }

  @Test
  public void dirAsFilenameResultsInDefault() throws IOException {
    final File file = appender.getDatabaseFile(tmp.newFolder().getAbsolutePath());
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is("logback.db"));
  }

  @Test
  public void nullFilenameResultsInDefault() throws IOException {
    final File file = appender.getDatabaseFile(null);
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is("logback.db"));
  }

  @Test
  public void emptyFilenameResultsInDefault() throws IOException {
    final File file = appender.getDatabaseFile("");
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is("logback.db"));
  }

  @Test
  public void blankFilenameResultsInDefault() throws IOException {
    final File file = appender.getDatabaseFile("  ");
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is("logback.db"));
  }

  @Test
  public void setsDatabaseFilename() throws IOException {
    final File tmpFile = tmp.newFile();
    final File file = appender.getDatabaseFile(tmpFile.getAbsolutePath());
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(tmpFile.getName()));
  }

  @Test
  public void getMaxHistoryReturnsOriginalSetting() {
    // note that Duration.toString() returns units in "milliseconds",
    // "seconds", "minutes", or "hours"
    appender.setMaxHistory("800 milli");
    assertThat(appender.getMaxHistory(), containsString("800 milli"));
    appender.setMaxHistory("500 seconds");
    assertThat(appender.getMaxHistory(), containsString("8 minutes"));
    appender.setMaxHistory("120 minutes");
    assertThat(appender.getMaxHistory(), containsString("2 hours"));
    appender.setMaxHistory("1 hour");
    assertThat(appender.getMaxHistory(), containsString("1 hour"));
    appender.setMaxHistory("7 days");
    assertThat(appender.getMaxHistory(), containsString("168 hours"));
  }

  @Test
  public void maxHistorySetsMilliseconds() {
    appender.setMaxHistory("800 milli");
    assertThat(appender.getMaxHistoryMs(), is(800L));
    appender.setMaxHistory("500 seconds");
    assertThat(appender.getMaxHistoryMs(), is(500 * 1000L));
    appender.setMaxHistory("120 minutes");
    assertThat(appender.getMaxHistoryMs(), is(120 * 60 * 1000L));
    appender.setMaxHistory("1 hour");
    assertThat(appender.getMaxHistoryMs(), is(60 * 60 * 1000L));
    appender.setMaxHistory("7 days");
    assertThat(appender.getMaxHistoryMs(), is(7 * 24 * 60 * 60 * 1000L));
  }

  @Test
  public void getMaxHistoryEmptyByDefault() {
    assertThat(appender.getMaxHistory(), is(""));
  }

  @Test
  public void getMaxHistoryMsZeroByDefault() {
    assertThat(appender.getMaxHistoryMs(), is(0L));
  }

  private void configureLogbackByString(String xml) throws JoranException {
    JoranConfigurator config = new JoranConfigurator();
    config.setContext(context);

    InputStream stream = new ByteArrayInputStream(xml.getBytes());
    config.doConfigure(stream);

    StatusUtil statusUtil = new StatusUtil(context);
    if (statusUtil.getHighestLevel(0) == ErrorStatus.ERROR) {
      fail("Configuration error found");
    }
  }

  private void addLogEvents(int count, long delayMs) throws InterruptedException {
    Logger log = context.getLogger(SQLiteAppenderTest.class);
    startTimeMs = System.currentTimeMillis();
    for (int i = 0; i < count; i++) {
      log.info("i={}", i);

      if (delayMs > 0) {
        Thread.sleep(delayMs);
      }
    }
  }

  /** Gets a SQLiteAppender with a no-op log-cleaner mock */
  private void addAppenderToContext(String maxHistory) {
    appender.setMaxHistory(maxHistory);
    appender.setLogCleaner(logCleaner);
    appender.start();
    ch.qos.logback.classic.Logger testRoot = context.getLogger(SQLiteAppenderTest.class);
    testRoot.addAppender(appender);
  }

  private Cursor getCursor() {
    SQLiteDatabase db = SQLiteDatabase.openDatabase(TEST_SQLITE_FILENAME, null, SQLiteDatabase.OPEN_READONLY);
    return db.rawQuery("SELECT timestmp FROM logging_event;", null);
  }

  private void assertRowCount(Cursor c, int min, int max) {
    assertThat(c.getCount(), is(greaterThan(min)));
    assertThat(c.getCount(), is(lessThan(max)));
  }
}
