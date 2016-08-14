/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.android;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import ch.qos.logback.classic.db.SQLBuilder;
import ch.qos.logback.classic.db.names.DBNameResolver;
import ch.qos.logback.classic.db.names.DefaultDBNameResolver;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.android.CommonPathUtil;
import ch.qos.logback.core.util.Duration;

/**
 * SQLiteAppender is a logback appender optimized for Android SQLite. It requires no JDBC
 * as it uses the built-in Android SQLite API.
 *
 * @author Anthony Trinh
 * @since 1.0.11
 */
public class SQLiteAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private SQLiteDatabase db;
  private String insertPropertiesSQL;
  private String insertExceptionSQL;
  private String insertSQL;
  private String filename;
  private DBNameResolver dbNameResolver;
  private Duration maxHistory;
  private long lastCleanupTime = 0;
  private SQLiteLogCleaner logCleaner;

  /**
   * Sets the database name resolver, used to customize the names of the table names
   * and columns in the database.
   *
   * @param dbNameResolver the desired database name resolver
   */
  public void setDbNameResolver(DBNameResolver dbNameResolver) {
    this.dbNameResolver = dbNameResolver;
  }

  /**
   * Get the maximum history in time duration of records to keep
   *
   * @return max history in time duration (e.g., "1 day")
   */
  public String getMaxHistory() {
    return maxHistory != null ? maxHistory.toString() : "";
  }

  /**
   * Gets the maximum history in milliseconds
   * @return the max history in milliseconds
   */
  public long getMaxHistoryMs() {
    return maxHistory != null ? maxHistory.getMilliseconds() : 0;
  }

  /**
   * Set the maximum history in time duration of records to keep
   *
   * @param maxHistory
   *                max history in time duration (e.g., "2 days")
   */
  public void setMaxHistory(String maxHistory) {
    this.maxHistory = Duration.valueOf(maxHistory);
  }

  /**
   * Gets the absolute path to the SQLite database
   * @return
     */
  public String getFilename() {
    return this.filename;
  }

  /**
   * Sets the path to the destination SQLite database
   * @param filename absolute path to file
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * Gets a file object from a file path to a SQLite database
   * @param filename absolute path to database file
   * @return the file object if a valid file found; otherwise, null
   */
  public File getDatabaseFile(String filename) {
    File dbFile = null;
    if (filename != null && filename.trim().length() > 0) {
      dbFile = new File(filename);
    }
    if (dbFile == null || dbFile.isDirectory()) {
      if (getContext() != null) {
        final String packageName = getContext().getProperty(CoreConstants.PACKAGE_NAME_KEY);
        if (packageName != null && packageName.trim().length() > 0) {
          dbFile = new File(CommonPathUtil.getDatabaseDirectoryPath(packageName), "logback.db");
        }
      } else {
        dbFile = null;
      }
    }
    return dbFile;
  }

  /*
   * (non-Javadoc)
   * @see ch.qos.logback.core.UnsynchronizedAppenderBase#start()
   */
  @Override
  public void start() {
    this.started = false;

    File dbfile = getDatabaseFile(this.filename);
    if (dbfile == null) {
      addError("Cannot determine database filename");
      return;
    }

    boolean dbOpened = false;
    try {
      dbfile.getParentFile().mkdirs();
      addInfo("db path: " + dbfile.getAbsolutePath());
      this.db = SQLiteDatabase.openOrCreateDatabase(dbfile.getPath(), null);
      dbOpened = true;
    } catch (SQLiteException e) {
      addError("Cannot open database", e);
    }

    if (dbOpened) {
      if (dbNameResolver == null) {
        dbNameResolver = new DefaultDBNameResolver();
      }

      insertExceptionSQL = SQLBuilder.buildInsertExceptionSQL(dbNameResolver);
      insertPropertiesSQL = SQLBuilder.buildInsertPropertiesSQL(dbNameResolver);
      insertSQL = SQLBuilder.buildInsertSQL(dbNameResolver);

      try {
        this.db.execSQL(SQLBuilder.buildCreateLoggingEventTableSQL(dbNameResolver));
        this.db.execSQL(SQLBuilder.buildCreatePropertyTableSQL(dbNameResolver));
        this.db.execSQL(SQLBuilder.buildCreateExceptionTableSQL(dbNameResolver));

        clearExpiredLogs(this.db);

        super.start();

        this.started = true;
      } catch (SQLiteException e) {
        addError("Cannot create database tables", e);
      }
    }
  }

  /**
   * Removes expired logs from the database
   * @param db
   */
  private void clearExpiredLogs(SQLiteDatabase db) {
    if (lastCheckExpired(this.maxHistory, this.lastCleanupTime)) {
      this.lastCleanupTime = System.currentTimeMillis();
      this.getLogCleaner().performLogCleanup(db, this.maxHistory);
    }
  }

  /**
   * Determines whether it's time to clear expired logs
   * @param expiry max time duration between checks
   * @param lastCleanupTime timestamp (ms) of last cleanup
   * @return true if last check has expired
   */
  private boolean lastCheckExpired(Duration expiry, long lastCleanupTime) {
    boolean isExpired = false;
    if (expiry != null && expiry.getMilliseconds() > 0) {
      final long now = System.currentTimeMillis();
      final long timeDiff = now - lastCleanupTime;
      isExpired = (lastCleanupTime <= 0) || (timeDiff >= expiry.getMilliseconds());
    }
    return isExpired;
  }

  /**
   * Gets the {@code SQLiteLogCleaner} in use. Creates default if needed.
   */
  public SQLiteLogCleaner getLogCleaner() {
    if (this.logCleaner == null) {
      this.logCleaner = new SQLiteLogCleaner() {
        @Override
        public void performLogCleanup(SQLiteDatabase db, Duration expiry) {
          final long expiryMs = System.currentTimeMillis() - expiry.getMilliseconds();
          final String deleteExpiredLogsSQL = SQLBuilder.buildDeleteExpiredLogsSQL(dbNameResolver, expiryMs);
          db.execSQL(deleteExpiredLogsSQL);
        }
      };
    }
    return this.logCleaner;
  }

  /**
   * Sets the {@code SQLiteLogCleaner}, invoked when {@code maxHistory} is exceeded
   * at startup and in between logging events
   * @param logCleaner
   */
  public void setLogCleaner(SQLiteLogCleaner logCleaner) {
    this.logCleaner = logCleaner;
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() throws Throwable {
    this.db.close();
  }

  /*
   * (non-Javadoc)
   * @see ch.qos.logback.core.UnsynchronizedAppenderBase#stop()
   */
  @Override
  public void stop() {
    this.db.close();
    this.lastCleanupTime = 0;
  }

  /*
   * (non-Javadoc)
   * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
   */
  @Override
  public void append(ILoggingEvent eventObject) {
    if (isStarted()) {
      try {
        clearExpiredLogs(db);
        SQLiteStatement stmt = db.compileStatement(insertSQL);
        try {
          db.beginTransaction();
          long eventId = subAppend(eventObject, stmt);
          if (eventId != -1) {
            secondarySubAppend(eventObject, eventId);
            db.setTransactionSuccessful();
          }
        } finally {
          if (db.inTransaction()) {
            db.endTransaction();
          }
          stmt.close();
        }
      } catch (Throwable e) {
        addError("Cannot append event", e);
      }
    }
  }

  /**
   * Inserts the main details of a log event into the database
   *
   * @param event the event to insert
   * @param insertStatement the SQLite statement used to insert the event
   * @return the row ID of the newly inserted event; -1 if the insertion failed
   * @throws SQLException
   */
  private long subAppend(ILoggingEvent event, SQLiteStatement insertStatement) throws SQLException {

    bindLoggingEvent(insertStatement, event);
    bindLoggingEventArguments(insertStatement, event.getArgumentArray());

    // This is expensive... should we do it every time?
    bindCallerData(insertStatement, event.getCallerData());

    long insertId = -1;
    try {
      insertId = insertStatement.executeInsert();
    } catch (SQLiteException e) {
      addWarn("Failed to insert loggingEvent", e);
    }
    return insertId;
  }

  /**
   * Updates an existing row of an event with the secondary details of the event.
   * This includes MDC properties and any exception information.
   *
   * @param event the event containing the details to insert
   * @param eventId the row ID of the event to modify
   * @throws SQLException
   */
  private void secondarySubAppend(ILoggingEvent event, long eventId) throws SQLException {
    Map<String, String> mergedMap = mergePropertyMaps(event);
    insertProperties(mergedMap, eventId);

    if (event.getThrowableProxy() != null) {
      insertThrowable(event.getThrowableProxy(), eventId);
    }
  }

  private static final int  TIMESTMP_INDEX = 1;
  private static final int  FORMATTED_MESSAGE_INDEX  = 2;
  private static final int  LOGGER_NAME_INDEX = 3;
  private static final int  LEVEL_STRING_INDEX = 4;
  private static final int  THREAD_NAME_INDEX = 5;
  private static final int  REFERENCE_FLAG_INDEX = 6;
  private static final int  ARG0_INDEX = 7;
//  private static final int  ARG1_INDEX = 8;
//  private static final int  ARG2_INDEX = 9;
//  private static final int  ARG3_INDEX = 10;
  private static final int  CALLER_FILENAME_INDEX = 11;
  private static final int  CALLER_CLASS_INDEX = 12;
  private static final int  CALLER_METHOD_INDEX = 13;
  private static final int  CALLER_LINE_INDEX = 14;
//  private static final int  EVENT_ID_INDEX  = 15;

  /**
   * Binds the main details of a log event to a SQLite statement's parameters
   *
   * @param stmt the SQLite statement to modify
   * @param event the event containing the details to bind
   * @throws SQLException
   */
  private void bindLoggingEvent(SQLiteStatement stmt, ILoggingEvent event) throws SQLException {
    stmt.bindLong(TIMESTMP_INDEX, event.getTimeStamp());
    stmt.bindString(FORMATTED_MESSAGE_INDEX, event.getFormattedMessage());
    stmt.bindString(LOGGER_NAME_INDEX, event.getLoggerName());
    stmt.bindString(LEVEL_STRING_INDEX, event.getLevel().toString());
    stmt.bindString(THREAD_NAME_INDEX, event.getThreadName());
    stmt.bindLong(REFERENCE_FLAG_INDEX, computeReferenceMask(event));
  }

  /**
   * Binds a logging event's arguments (e.g., <code>logger.debug("x={} y={}", arg1, arg2)</code>)
   * to a SQLite statement's parameters
   *
   * @param stmt the SQLite statement to modify
   * @param argArray the argument array to bind
   * @throws SQLException
   */
  private void bindLoggingEventArguments(SQLiteStatement stmt, Object[] argArray) throws SQLException {
    int arrayLen = argArray != null ? argArray.length : 0;
    for (int i = 0; i < arrayLen && i < 4; i++) {
      stmt.bindString(ARG0_INDEX+i, asStringTruncatedTo254(argArray[i]));
    }
//
//    // set remaining columns to ""
//    for (int i = arrayLen; i < 4; i++) {
//      stmt.bindString(ARG0_INDEX+i, "");
//    }
  }

  /**
   * Gets the first 254 characters of an object's string representation. This is
   * used to truncate a logging event's argument binding if necessary.
   *
   * @param o the object
   * @return up to 254 characters of the object's string representation; or empty
   * string if the object string is itself null
   */
  private String asStringTruncatedTo254(Object o) {
    String s = null;
    if (o != null) {
      s = o.toString();
    }
    if (s != null && s.length() > 254) {
      s = s.substring(0, 254);
    }
    return s == null ? "" : s;
  }

  private static final short PROPERTIES_EXIST = 0x01;
  private static final short EXCEPTION_EXISTS = 0x02;

  /**
   * Computes the reference mask for a logging event, including
   * flags to indicate whether MDC properties or exception info
   * is available for the event.
   *
   * @param event the logging event to evaluate
   * @return the 16-bit reference mask
   */
  private static short computeReferenceMask(ILoggingEvent event) {
    short mask = 0;

    int mdcPropSize = 0;
    if (event.getMDCPropertyMap() != null) {
      mdcPropSize = event.getMDCPropertyMap().keySet().size();
    }
    int contextPropSize = 0;
    if (event.getLoggerContextVO().getPropertyMap() != null) {
      contextPropSize = event.getLoggerContextVO().getPropertyMap().size();
    }

    if (mdcPropSize > 0 || contextPropSize > 0) {
      mask = PROPERTIES_EXIST;
    }
    if (event.getThrowableProxy() != null) {
      mask |= EXCEPTION_EXISTS;
    }
    return mask;
  }

  /**
   * Merges a log event's properties with the properties of the logger context.
   * The context properties are first in the map, and then the event's properties
   * are appended.
   *
   * @param event the logging event to evaluate
   * @return the merged properties map
   */
  private Map<String, String> mergePropertyMaps(ILoggingEvent event) {
    Map<String, String> mergedMap = new HashMap<String, String>();
    // we add the context properties first, then the event properties, since
    // we consider that event-specific properties should have priority over
    // context-wide properties.
    Map<String, String> loggerContextMap = event.getLoggerContextVO().getPropertyMap();
    if (loggerContextMap != null) {
      mergedMap.putAll(loggerContextMap);
    }

    Map<String, String> mdcMap = event.getMDCPropertyMap();
    if (mdcMap != null) {
      mergedMap.putAll(mdcMap);
    }

    return mergedMap;
  }

  /**
   * Updates an existing row with property details (context properties and event's properties).
   *
   * @param mergedMap the properties of the context plus the event's properties
   * @param eventId the row ID of the event
   * @throws SQLException
   */
  private void insertProperties(Map<String, String> mergedMap, long eventId) throws SQLException {
    if (mergedMap.size() > 0) {
      SQLiteStatement stmt = db.compileStatement(insertPropertiesSQL);
      try {
        for (Entry<String,String> entry : mergedMap.entrySet()) {
          stmt.bindLong(1, eventId);
          stmt.bindString(2, entry.getKey());
          stmt.bindString(3, entry.getValue());
          stmt.executeInsert();
        }
      } finally {
        stmt.close();
      }
    }
  }

  /**
   * Binds the calling function's details (filename, line, etc.) to a SQLite statement's arguments
   *
   * @param stmt the SQLite statement to modify
   * @param callerDataArray the caller's stack trace
   * @throws SQLException
   */
  private void bindCallerData(SQLiteStatement stmt, StackTraceElement[] callerDataArray) throws SQLException {
    if (callerDataArray != null && callerDataArray.length > 0) {
      StackTraceElement callerData = callerDataArray[0];
      if (callerData != null) {
        stmt.bindString(CALLER_FILENAME_INDEX, callerData.getFileName());
        stmt.bindString(CALLER_CLASS_INDEX, callerData.getClassName());
        stmt.bindString(CALLER_METHOD_INDEX, callerData.getMethodName());
        stmt.bindString(CALLER_LINE_INDEX, Integer.toString(callerData.getLineNumber()));
      }
    }
  }

  /**
   * Inserts an exception into the logging_exceptions table
   *
   * @param stmt
   * @param txt
   * @param i
   * @param eventId
   */
  private void insertException(SQLiteStatement stmt, String txt, short i, long eventId) throws SQLException {
    stmt.bindLong(1, eventId);
    stmt.bindLong(2, i);
    stmt.bindString(3, txt);
    stmt.executeInsert();
  }

  private void insertThrowable(IThrowableProxy tp, long eventId) throws SQLException {

    SQLiteStatement stmt = db.compileStatement(insertExceptionSQL);
    try {
      short baseIndex = 0;
      while (tp != null) {
        StringBuilder buf = new StringBuilder();
        ThrowableProxyUtil.subjoinFirstLine(buf, tp);
        insertException(stmt, buf.toString(), baseIndex++, eventId);

        int commonFrames = tp.getCommonFrames();
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();

        for (int i = 0; i < stepArray.length - commonFrames; i++) {
          StringBuilder sb = new StringBuilder();
          sb.append(CoreConstants.TAB);
          ThrowableProxyUtil.subjoinSTEP(sb, stepArray[i]);
          insertException(stmt, sb.toString(), baseIndex++, eventId);
        }

        if (commonFrames > 0) {
          StringBuilder sb = new StringBuilder();
          sb.append(CoreConstants.TAB)
            .append("... ")
            .append(commonFrames)
            .append(" common frames omitted");

          insertException(stmt, sb.toString(), baseIndex++, eventId);
        }

        tp = tp.getCause();
      }
    } finally {
      stmt.close();
    }
  }
}
