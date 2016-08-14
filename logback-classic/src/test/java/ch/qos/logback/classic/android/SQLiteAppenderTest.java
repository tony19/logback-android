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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class SQLiteAppenderTest {

  private static final String TEST_SQLITE_FILENAME = CoreTestConstants.OUTPUT_DIR_PREFIX + "SQLiteAppenderTest/logback.db";
  private static final long EXPIRY_MS = 500;

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
  private long startTimeMs;
  private LoggerContext context;
  private SQLiteAppender appender;

  @Before
  public void setup() throws Exception {
    context = new LoggerContext();
    context.putProperty(CoreConstants.PACKAGE_NAME_KEY, "com.example");
    appender = new SQLiteAppender();
    appender.setContext(context);
  }

  @After
  public void teardown() {
    new File(TEST_SQLITE_FILENAME).delete();
  }

  @Test
  public void maxHistoryRemovesExpiredLogs() throws InterruptedException, JoranException {
    configureLogbackByString(SQLITE_APPENDER_WITH_MAX_HISTORY_XML);
    addLogEvents(12, EXPIRY_MS / 2);

    SQLiteDatabase db = SQLiteDatabase.openDatabase(TEST_SQLITE_FILENAME, null, SQLiteDatabase.OPEN_READONLY);
    Cursor c = db.rawQuery("SELECT timestmp FROM logging_event;", null);
    assertThat(c.getCount(), is(greaterThan(0)));

    final int colIndex = c.getColumnIndex("timestmp");
    while (c.moveToNext()) {
      final long timestamp = c.getLong(colIndex);
      assertThat(timestamp, is(greaterThan(startTimeMs - EXPIRY_MS)));
    }
  }

  @Test
  public void maxHistoryIsDisabledByDefault() throws InterruptedException, JoranException {
    configureLogbackByString(SQLITE_APPENDER_WITHOUT_MAX_HISTORY_XML);
    addLogEvents(1000, 0);

    SQLiteDatabase db = SQLiteDatabase.openDatabase(TEST_SQLITE_FILENAME, null, SQLiteDatabase.OPEN_READONLY);
    Cursor c = db.rawQuery("SELECT timestmp FROM logging_event;", null);
    assertThat(c.getCount(), is(1000));
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
}
