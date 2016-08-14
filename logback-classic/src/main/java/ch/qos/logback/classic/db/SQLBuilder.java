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
package ch.qos.logback.classic.db;

import ch.qos.logback.classic.db.names.*;

/**
 * @author Tomasz Nurkiewicz
 * @since 2010-03-16
 */
public class SQLBuilder {

  public static String buildInsertPropertiesSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_PROPERTY)).append(" (");
    sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ");
    sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.MAPPED_KEY)).append(", ");
    sqlBuilder.append(dbNameResolver.getColumnName(ColumnName.MAPPED_VALUE)).append(") ");
    sqlBuilder.append("VALUES (?, ?, ?)");
    return sqlBuilder.toString();
  }

  public static String buildInsertExceptionSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_EXCEPTION)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.I)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.TRACE_LINE)).append(") ")
        .append("VALUES (?, ?, ?)");
    return sqlBuilder.toString();
  }

  public static String buildInsertSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.TIMESTMP)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.FORMATTED_MESSAGE)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.LOGGER_NAME)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.LEVEL_STRING)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.THREAD_NAME)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.REFERENCE_FLAG)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG0)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG1)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG2)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG3)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_FILENAME)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_CLASS)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_METHOD)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_LINE)).append(") ")
        .append("VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    return sqlBuilder.toString();
  }

  public static String buildCreateLoggingEventTableSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.TIMESTMP)).append(" BIGINT NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.FORMATTED_MESSAGE)).append(" TEXT NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.LOGGER_NAME)).append(" VARCHAR(254) NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.LEVEL_STRING)).append(" VARCHAR(254) NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.THREAD_NAME)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.REFERENCE_FLAG)).append(" SMALLINT, ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG0)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG1)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG2)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.ARG3)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_FILENAME)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_CLASS)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_METHOD)).append(" VARCHAR(254), ")
        .append(dbNameResolver.getColumnName(ColumnName.CALLER_LINE)).append(" CHAR(4), ")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT")
        .append(")");
    return sqlBuilder.toString();
  }

  public static String buildCreatePropertyTableSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_PROPERTY)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(" BIGINT NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.MAPPED_KEY)).append(" VARCHAR(254) NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.MAPPED_VALUE)).append(" VARCHAR(254) NOT NULL, ")
        .append("PRIMARY KEY (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.MAPPED_KEY)).append("), ")
        .append("FOREIGN KEY (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(") ")
        .append("REFERENCES ")
        .append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(") ")
        .append(")");
    return sqlBuilder.toString();
  }

  public static String buildCreateExceptionTableSQL(DBNameResolver dbNameResolver) {
    StringBuilder sqlBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
    sqlBuilder.append(dbNameResolver.getTableName(TableName.LOGGING_EVENT_EXCEPTION)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(" BIGINT NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.I)).append(" SMALLINT NOT NULL, ")
        .append(dbNameResolver.getColumnName(ColumnName.TRACE_LINE)).append(" VARCHAR(254) NOT NULL, ")
        .append("PRIMARY KEY (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(", ")
        .append(dbNameResolver.getColumnName(ColumnName.I)).append("), ")
        .append("FOREIGN KEY (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(") ")
        .append("REFERENCES ")
        .append(dbNameResolver.getTableName(TableName.LOGGING_EVENT)).append(" (")
        .append(dbNameResolver.getColumnName(ColumnName.EVENT_ID)).append(") ")
        .append(")");
    return sqlBuilder.toString();
  }

  public static String buildDeleteExpiredLogsSQL(DBNameResolver dbNameResolver, long expiryMs) {
    StringBuilder sqlBuilder = new StringBuilder("DELETE FROM ")
            .append(dbNameResolver.getTableName(TableName.LOGGING_EVENT))
            .append(" WHERE ").append(dbNameResolver.getColumnName(ColumnName.TIMESTMP))
            .append(" <= ").append(expiryMs)
            .append(";");
    return sqlBuilder.toString();
  }
}
