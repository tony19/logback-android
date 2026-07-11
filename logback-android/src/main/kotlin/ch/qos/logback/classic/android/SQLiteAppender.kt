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
package ch.qos.logback.classic.android

import java.io.File

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteStatement
import ch.qos.logback.classic.db.SQLBuilder
import ch.qos.logback.classic.db.names.DBNameResolver
import ch.qos.logback.classic.db.names.DefaultDBNameResolver
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.android.AndroidContextUtil
import ch.qos.logback.core.util.Duration

/**
 * SQLiteAppender is a logback appender optimized for Android SQLite. It requires no JDBC
 * as it uses the built-in Android SQLite API.
 *
 * @author Anthony Trinh
 * @since 1.0.11
 */
public open class SQLiteAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {

    private var db: SQLiteDatabase? = null
    private lateinit var insertPropertiesSQL: String
    private lateinit var insertExceptionSQL: String
    private lateinit var insertSQL: String
    private var maxHistoryDuration: Duration? = null
    private var lastCleanupTime: Long = 0
    private var clock: Clock = SystemClock()

    /**
     * The database name resolver, used to customize the names of the
     * table names and columns in the database.
     */
    public var dbNameResolver: DBNameResolver? = null

    /**
     * The absolute path to the SQLite database
     */
    public var filename: String? = null

    /**
     * The maximum history in time duration (e.g., "1 day") of records to keep
     */
    public var maxHistory: String
        get() = maxHistoryDuration?.toString() ?: ""
        set(value) {
            maxHistoryDuration = Duration.valueOf(value)
        }

    /**
     * The maximum history in milliseconds
     */
    public val maxHistoryMs: Long
        get() = maxHistoryDuration?.milliseconds ?: 0

    /**
     * The [SQLiteLogCleaner] invoked when [maxHistory] is exceeded at
     * startup and in between logging events. Reading this property creates
     * the default log cleaner if none was set.
     */
    public var logCleaner: SQLiteLogCleaner? = null
        get() {
            if (field == null) {
                field = SQLiteLogCleaner { db, expiry ->
                    val expiryMs = clock.currentTimeMillis() - expiry.milliseconds
                    db.execSQL(SQLBuilder.buildDeleteExpiredLogsSQL(dbNameResolver, expiryMs))
                }
            }
            return field
        }

    @JvmName("setClock")
    internal fun setClock(clock: Clock) {
        this.clock = clock
    }

    /**
     * Gets a file object from a file path to a SQLite database
     *
     * @param filename absolute path to database file
     * @return the file object if a valid file found; otherwise, null
     */
    public fun getDatabaseFile(filename: String?): File? {
        var dbFile: File? = null
        if (!filename.isNullOrBlank()) {
            dbFile = File(filename)
        }
        if (dbFile == null || dbFile.isDirectory) {
            dbFile = File(AndroidContextUtil().getDatabasePath("logback.db"))
        }
        return dbFile
    }

    override fun start() {
        this.started = false

        val dbFile = getDatabaseFile(this.filename)
        if (dbFile == null) {
            addError("Cannot determine database filename")
            return
        }

        val db = try {
            dbFile.parentFile?.mkdirs()
            addInfo("db path: ${dbFile.absolutePath}")
            SQLiteDatabase.openOrCreateDatabase(dbFile.path, null)
        } catch (e: SQLiteException) {
            addError("Cannot open database", e)
            return
        }
        this.db = db

        val dbNameResolver = this.dbNameResolver ?: DefaultDBNameResolver().also {
            this.dbNameResolver = it
        }

        insertExceptionSQL = SQLBuilder.buildInsertExceptionSQL(dbNameResolver)
        insertPropertiesSQL = SQLBuilder.buildInsertPropertiesSQL(dbNameResolver)
        insertSQL = SQLBuilder.buildInsertSQL(dbNameResolver)

        try {
            db.execSQL(SQLBuilder.buildCreateLoggingEventTableSQL(dbNameResolver))
            db.execSQL(SQLBuilder.buildCreatePropertyTableSQL(dbNameResolver))
            db.execSQL(SQLBuilder.buildCreateExceptionTableSQL(dbNameResolver))

            clearExpiredLogs(db)

            super.start()

            this.started = true
        } catch (e: SQLiteException) {
            addError("Cannot create database tables", e)
        }
    }

    override fun stop() {
        db?.close()
        this.lastCleanupTime = 0
    }

    public override fun append(eventObject: ILoggingEvent) {
        if (!isStarted) {
            return
        }
        val db = this.db ?: return

        try {
            clearExpiredLogs(db)
            val stmt = db.compileStatement(insertSQL)
            try {
                db.beginTransaction()
                val eventId = subAppend(eventObject, stmt)
                if (eventId != -1L) {
                    secondarySubAppend(eventObject, eventId)
                    db.setTransactionSuccessful()
                }
            } finally {
                if (db.inTransaction()) {
                    db.endTransaction()
                }
                stmt.close()
            }
        } catch (e: Throwable) {
            addError("Cannot append event", e)
        }
    }

    /**
     * Removes expired logs from the database
     */
    private fun clearExpiredLogs(db: SQLiteDatabase) {
        val maxHistory = this.maxHistoryDuration
        if (lastCheckExpired(maxHistory, this.lastCleanupTime)) {
            this.lastCleanupTime = clock.currentTimeMillis()
            logCleaner?.performLogCleanup(db, maxHistory!!)
        }
    }

    /**
     * Determines whether it's time to clear expired logs
     *
     * @param expiry max time duration between checks
     * @param lastCleanupTime timestamp (ms) of last cleanup
     * @return true if last check has expired
     */
    private fun lastCheckExpired(expiry: Duration?, lastCleanupTime: Long): Boolean {
        if (expiry == null || expiry.milliseconds <= 0) {
            return false
        }
        val timeDiff = clock.currentTimeMillis() - lastCleanupTime
        return lastCleanupTime <= 0 || timeDiff >= expiry.milliseconds
    }

    /**
     * Inserts the main details of a log event into the database
     *
     * @param event the event to insert
     * @param insertStatement the SQLite statement used to insert the event
     * @return the row ID of the newly inserted event; -1 if the insertion failed
     */
    private fun subAppend(event: ILoggingEvent, insertStatement: SQLiteStatement): Long {
        bindLoggingEvent(insertStatement, event)
        bindLoggingEventArguments(insertStatement, event.argumentArray)

        // This is expensive... should we do it every time?
        bindCallerData(insertStatement, event.callerData)

        return try {
            insertStatement.executeInsert()
        } catch (e: SQLiteException) {
            addWarn("Failed to insert loggingEvent", e)
            -1
        }
    }

    /**
     * Updates an existing row of an event with the secondary details of the event.
     * This includes MDC properties and any exception information.
     *
     * @param event the event containing the details to insert
     * @param eventId the row ID of the event to modify
     */
    private fun secondarySubAppend(event: ILoggingEvent, eventId: Long) {
        insertProperties(mergePropertyMaps(event), eventId)

        event.throwableProxy?.let { throwableProxy ->
            insertThrowable(throwableProxy, eventId)
        }
    }

    /**
     * Binds the main details of a log event to a SQLite statement's parameters
     *
     * @param stmt the SQLite statement to modify
     * @param event the event containing the details to bind
     */
    private fun bindLoggingEvent(stmt: SQLiteStatement, event: ILoggingEvent) {
        stmt.bindLong(TIMESTMP_INDEX, event.timeStamp)
        stmt.bindString(FORMATTED_MESSAGE_INDEX, event.formattedMessage)
        stmt.bindString(LOGGER_NAME_INDEX, event.loggerName)
        stmt.bindString(LEVEL_STRING_INDEX, event.level.toString())
        stmt.bindString(THREAD_NAME_INDEX, event.threadName)
        stmt.bindLong(REFERENCE_FLAG_INDEX, computeReferenceMask(event).toLong())
    }

    /**
     * Binds a logging event's arguments (e.g., `logger.debug("x={} y={}", arg1, arg2)`)
     * to a SQLite statement's parameters
     *
     * @param stmt the SQLite statement to modify
     * @param argArray the argument array to bind
     */
    private fun bindLoggingEventArguments(stmt: SQLiteStatement, argArray: Array<Any?>?) {
        val arrayLen = argArray?.size ?: 0
        for (i in 0 until minOf(arrayLen, 4)) {
            stmt.bindString(ARG0_INDEX + i, asStringTruncatedTo254(argArray!![i]))
        }
    }

    /**
     * Gets the first 254 characters of an object's string representation. This is
     * used to truncate a logging event's argument binding if necessary.
     *
     * @param o the object
     * @return up to 254 characters of the object's string representation; or empty
     * string if the object string is itself null
     */
    private fun asStringTruncatedTo254(o: Any?): String {
        val s = o?.toString() ?: return ""
        return if (s.length > 254) s.substring(0, 254) else s
    }

    /**
     * Merges a log event's properties with the properties of the logger context.
     * The context properties are first in the map, and then the event's properties
     * are appended.
     *
     * @param event the logging event to evaluate
     * @return the merged properties map
     */
    private fun mergePropertyMaps(event: ILoggingEvent): Map<String, String> {
        val mergedMap = mutableMapOf<String, String>()
        // we add the context properties first, then the event properties, since
        // we consider that event-specific properties should have priority over
        // context-wide properties.
        event.loggerContextVO.propertyMap?.let(mergedMap::putAll)
        event.mdcPropertyMap?.let(mergedMap::putAll)
        return mergedMap
    }

    /**
     * Updates an existing row with property details (context properties and event's properties).
     *
     * @param mergedMap the properties of the context plus the event's properties
     * @param eventId the row ID of the event
     */
    private fun insertProperties(mergedMap: Map<String, String>, eventId: Long) {
        if (mergedMap.isEmpty()) {
            return
        }
        val db = this.db ?: return
        db.compileStatement(insertPropertiesSQL).use { stmt ->
            for ((key, value) in mergedMap) {
                stmt.bindLong(1, eventId)
                stmt.bindString(2, key)
                stmt.bindString(3, value)
                stmt.executeInsert()
            }
        }
    }

    /**
     * Binds the calling function's details (filename, line, etc.) to a SQLite statement's arguments
     *
     * @param stmt the SQLite statement to modify
     * @param callerDataArray the caller's stack trace
     */
    private fun bindCallerData(stmt: SQLiteStatement, callerDataArray: Array<StackTraceElement?>?) {
        val callerData = callerDataArray?.firstOrNull() ?: return
        bindString(stmt, CALLER_FILENAME_INDEX, callerData.fileName)
        bindString(stmt, CALLER_CLASS_INDEX, callerData.className)
        bindString(stmt, CALLER_METHOD_INDEX, callerData.methodName)
        bindString(stmt, CALLER_LINE_INDEX, callerData.lineNumber.toString())
    }

    private fun bindString(stmt: SQLiteStatement, columnIndex: Int, value: String?) {
        if (value != null) {
            stmt.bindString(columnIndex, value)
        }
    }

    /**
     * Inserts an exception into the logging_exceptions table
     */
    private fun insertException(stmt: SQLiteStatement, txt: String, i: Short, eventId: Long) {
        stmt.bindLong(1, eventId)
        stmt.bindLong(2, i.toLong())
        stmt.bindString(3, txt)
        stmt.executeInsert()
    }

    private fun insertThrowable(throwableProxy: IThrowableProxy, eventId: Long) {
        val db = this.db ?: return
        db.compileStatement(insertExceptionSQL).use { stmt ->
            var tp: IThrowableProxy? = throwableProxy
            var baseIndex: Short = 0
            while (tp != null) {
                val firstLine = StringBuilder().also { buf ->
                    ThrowableProxyUtil.subjoinFirstLine(buf, tp)
                }
                insertException(stmt, firstLine.toString(), baseIndex++, eventId)

                val commonFrames = tp.commonFrames
                val stepArray = tp.stackTraceElementProxyArray

                for (i in 0 until stepArray.size - commonFrames) {
                    val sb = StringBuilder().append(CoreConstants.TAB).also { buf ->
                        ThrowableProxyUtil.subjoinSTEP(buf, stepArray[i])
                    }
                    insertException(stmt, sb.toString(), baseIndex++, eventId)
                }

                if (commonFrames > 0) {
                    val sb = StringBuilder()
                        .append(CoreConstants.TAB)
                        .append("... ")
                        .append(commonFrames)
                        .append(" common frames omitted")

                    insertException(stmt, sb.toString(), baseIndex++, eventId)
                }

                tp = tp.cause
            }
        }
    }

    private companion object {
        private const val TIMESTMP_INDEX = 1
        private const val FORMATTED_MESSAGE_INDEX = 2
        private const val LOGGER_NAME_INDEX = 3
        private const val LEVEL_STRING_INDEX = 4
        private const val THREAD_NAME_INDEX = 5
        private const val REFERENCE_FLAG_INDEX = 6
        private const val ARG0_INDEX = 7
        private const val CALLER_FILENAME_INDEX = 11
        private const val CALLER_CLASS_INDEX = 12
        private const val CALLER_METHOD_INDEX = 13
        private const val CALLER_LINE_INDEX = 14

        private const val PROPERTIES_EXIST: Short = 0x01
        private const val EXCEPTION_EXISTS: Short = 0x02

        /**
         * Computes the reference mask for a logging event, including
         * flags to indicate whether MDC properties or exception info
         * is available for the event.
         *
         * @param event the logging event to evaluate
         * @return the 16-bit reference mask
         */
        private fun computeReferenceMask(event: ILoggingEvent): Short {
            var mask: Int = 0

            val mdcPropSize = event.mdcPropertyMap?.size ?: 0
            val contextPropSize = event.loggerContextVO.propertyMap?.size ?: 0

            if (mdcPropSize > 0 || contextPropSize > 0) {
                mask = PROPERTIES_EXIST.toInt()
            }
            if (event.throwableProxy != null) {
                mask = mask or EXCEPTION_EXISTS.toInt()
            }
            return mask.toShort()
        }
    }
}
