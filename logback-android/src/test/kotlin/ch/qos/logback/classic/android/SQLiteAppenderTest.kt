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

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteStatement
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.db.names.SimpleDBNameResolver
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggerContextVO
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.status.Status
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SQLiteAppenderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val logCleaner = mock<SQLiteLogCleaner>()
    private lateinit var context: LoggerContext
    private lateinit var appender: SQLiteAppender
    private lateinit var dbFile: File

    /** The time reported by the appender's clock */
    private var now = START_MS

    /** Statements compiled by a [mockDatabase], in order */
    private val compiledStatements = mutableListOf<SQLiteStatement>()

    @Before
    fun setUp() {
        context = LoggerContext()
        // the parent directory doesn't exist yet: start() must create it
        dbFile = File(tmp.root, "databases/logback.db")
        appender = newAppender()
    }

    @After
    fun tearDown() {
        if (appender.isStarted) {
            appender.stop()
        }
    }

    // --- maxHistory ---------------------------------------------------------

    @Test
    fun `maxHistory is empty by default`() {
        assertEquals("", appender.maxHistory)
    }

    @Test
    fun `maxHistoryMs is zero by default`() {
        assertEquals(0L, appender.maxHistoryMs)
    }

    @Test
    fun `maxHistory reports the setting in whole units`() {
        // Duration.toString() uses "milliseconds", "seconds", "minutes" or "hours"
        appender.maxHistory = "800 milli"
        assertEquals("800 milliseconds", appender.maxHistory)
        appender.maxHistory = "500 seconds"
        assertEquals("8 minutes", appender.maxHistory)
        appender.maxHistory = "120 minutes"
        assertEquals("2 hours", appender.maxHistory)
        appender.maxHistory = "1 hour"
        assertEquals("1 hours", appender.maxHistory)
        appender.maxHistory = "7 days"
        assertEquals("168 hours", appender.maxHistory)
    }

    @Test
    fun `maxHistory sets maxHistoryMs`() {
        appender.maxHistory = "800 milli"
        assertEquals(800L, appender.maxHistoryMs)
        appender.maxHistory = "500 seconds"
        assertEquals(500 * 1000L, appender.maxHistoryMs)
        appender.maxHistory = "120 minutes"
        assertEquals(120 * MINUTE_MS, appender.maxHistoryMs)
        appender.maxHistory = "1 hour"
        assertEquals(HOUR_MS, appender.maxHistoryMs)
        appender.maxHistory = "7 days"
        assertEquals(7 * 24 * HOUR_MS, appender.maxHistoryMs)
    }

    // --- getDatabaseFile ----------------------------------------------------

    @Test
    fun `getDatabaseFile uses the given file`() {
        val file = tmp.newFile()
        assertEquals(file, appender.getDatabaseFile(file.absolutePath))
    }

    @Test
    fun `getDatabaseFile falls back to the default database for a directory`() {
        assertEquals(defaultDatabaseFile(), appender.getDatabaseFile(tmp.newFolder().absolutePath))
    }

    @Test
    fun `getDatabaseFile falls back to the default database for a null filename`() {
        assertEquals(defaultDatabaseFile(), appender.getDatabaseFile(null))
    }

    @Test
    fun `getDatabaseFile falls back to the default database for an empty filename`() {
        assertEquals(defaultDatabaseFile(), appender.getDatabaseFile(""))
    }

    @Test
    fun `getDatabaseFile falls back to the default database for a blank filename`() {
        assertEquals(defaultDatabaseFile(), appender.getDatabaseFile(" \t\n "))
    }

    @Test
    fun `getDatabaseFile keeps a filename of control characters`() {
        // blank means whitespace (Char.isWhitespace), which control chars are not
        assertEquals(File("\u0001"), appender.getDatabaseFile("\u0001"))
    }

    @Test
    fun `getDatabaseFile falls back to the default database for a filename of Unicode spaces`() {
        assertEquals(defaultDatabaseFile(), appender.getDatabaseFile("\u2003"))
    }

    // --- start --------------------------------------------------------------

    @Test
    fun `start creates the database and its tables`() {
        appender.start()

        assertTrue(appender.isStarted)
        assertEquals(setOf("logging_event", "logging_event_property", "logging_event_exception"), tableNames())
        assertStatus(Status.INFO, "db path: ${dbFile.absolutePath}")
        assertEquals(emptyList(), problems())
    }

    @Test
    fun `start fails when the database file cannot be determined`() {
        val spied = spy(appender)
        doReturn(null).whenever(spied).getDatabaseFile(anyOrNull())

        spied.start()

        assertFalse(spied.isStarted)
        assertStatus(Status.ERROR, "Cannot determine database filename")
        assertFalse(dbFile.exists())
    }

    @Test
    fun `start fails when the database cannot be opened`() {
        // a regular file can't be the database's parent directory
        appender.filename = File(tmp.newFile("not-a-directory"), "logback.db").absolutePath

        appender.start()

        assertFalse(appender.isStarted)
        assertIs<SQLiteException>(assertStatus(Status.ERROR, "Cannot open database").throwable)
    }

    @Test
    fun `a failed restart leaves the appender stopped`() {
        appender.start()
        appender.filename = File(tmp.newFile("not-a-directory"), "logback.db").absolutePath

        // isStarted is still set from the first start
        appender.start()
        try {
            assertFalse(appender.isStarted)
            assertIs<SQLiteException>(assertStatus(Status.ERROR, "Cannot open database").throwable)
        } finally {
            appender.stop() // closes the database of the first start
        }
    }

    @Test
    fun `start fails when the tables cannot be created`() {
        // table names qualified with a database that isn't attached
        val configured = configureFromXml(
            """
            <dbNameResolver class="${SimpleDBNameResolver::class.java.name}">
              <tableNamePrefix>missing.</tableNamePrefix>
            </dbNameResolver>
            """
        )
        try {
            assertFalse(configured.isStarted)
            assertIs<SQLiteException>(assertStatus(Status.ERROR, "Cannot create database tables").throwable)
        } finally {
            configured.stop()
        }
    }

    @Test
    fun `start fails when the startup log cleanup fails`() {
        appender.maxHistory = "1 hour"
        appender.logCleaner = SQLiteLogCleaner { _, _ -> throw SQLiteFullException("database or disk is full") }

        appender.start()
        try {
            assertFalse(appender.isStarted)
            assertIs<SQLiteFullException>(assertStatus(Status.ERROR, "Cannot create database tables").throwable)
        } finally {
            appender.stop()
        }
    }

    @Test
    fun `configures from XML with a custom DB name resolver`() {
        val configured = configureFromXml(
            """
            <maxHistory>2 hours</maxHistory>
            <dbNameResolver class="${SimpleDBNameResolver::class.java.name}">
              <tableNamePrefix>app_</tableNamePrefix>
              <columnNamePrefix>c_</columnNamePrefix>
            </dbNameResolver>
            """
        )
        try {
            assertTrue(configured.isStarted)
            assertEquals(dbFile.absolutePath, configured.filename)
            assertEquals(2 * HOUR_MS, configured.maxHistoryMs)

            context.getLogger("xml").info("configured from XML")

            assertEquals(setOf("app_logging_event", "app_logging_event_property", "app_logging_event_exception"), tableNames())
            assertEquals(
                listOf("configured from XML"),
                rows("app_logging_event", "c_event_id").map { it["c_formatted_message"] },
            )
        } finally {
            configured.stop()
        }
    }

    // --- log cleanup --------------------------------------------------------

    @Test
    fun `cleanup occurs at appender startup`() {
        startWithMockCleaner("1 hour")

        verify(logCleaner).performLogCleanup(argThat { path == dbFile.absolutePath }, argThat { milliseconds == HOUR_MS })
    }

    @Test
    fun `cleanup occurs at startup even when the clock is near the epoch`() {
        // e.g. a device whose clock was reset to 1970: less than maxHistory since time 0
        now = MINUTE_MS

        startWithMockCleaner("1 hour")

        verify(logCleaner).performLogCleanup(any(), any())
    }

    @Test
    fun `cleanup does not occur before expiration`() {
        startWithMockCleaner("1 hour")

        appendEvents(3, stepMs = 0)

        // log cleanup normally runs between logging events once the expiry time
        // is exceeded, but nothing expired here, so only the startup cleanup ran
        verify(logCleaner, times(1)).performLogCleanup(any(), any())
        assertEquals(3, loggedEvents().size)
    }

    @Test
    fun `cleanup occurs after every expiration`() {
        startWithMockCleaner("$EXPIRY_MS milli")

        // an event every half expiry period: cleanup at startup, then every other event
        appendEvents(7, stepMs = EXPIRY_MS / 2)

        verify(logCleaner, times(4)).performLogCleanup(any(), any())
    }

    @Test
    fun `cleanup never occurs without maxHistory`() {
        startWithMockCleaner(maxHistory = null)

        appendEvents(3, stepMs = 365 * 24 * HOUR_MS)

        verify(logCleaner, never()).performLogCleanup(any(), any())
        assertEquals(3, loggedEvents().size)
    }

    @Test
    fun `cleanup never occurs with a zero maxHistory`() {
        startWithMockCleaner("0 seconds")

        appendEvents(3, stepMs = 365 * 24 * HOUR_MS)

        verify(logCleaner, never()).performLogCleanup(any(), any())
    }

    @Test
    fun `stop resets the cleanup schedule`() {
        startWithMockCleaner("1 hour")
        appender.doAppend(event("before restart"))

        appender.stop()
        now += MINUTE_MS
        appender.start()
        appender.doAppend(event("after restart"))

        // without the reset, the restart would be within the hour since the startup cleanup
        verify(logCleaner, times(2)).performLogCleanup(any(), any())
        assertEquals(listOf("before restart", "after restart"), messages())
    }

    @Test
    fun `a subclass without a log cleaner skips the cleanup`() {
        appender = object : SQLiteAppender() {
            override var logCleaner: SQLiteLogCleaner?
                get() = null
                set(_) {}
        }.also {
            it.setClock(Clock { now })
            it.filename = dbFile.absolutePath
            it.context = context
            it.maxHistory = "1 second"
        }
        appender.start()
        appender.doAppend(event("old"))

        now += 2 * 1000
        appender.doAppend(event("new"))

        assertEquals(listOf("old", "new"), messages())
    }

    @Test
    fun `a default log cleaner is created on demand`() {
        val cleaner = assertNotNull(appender.logCleaner)

        assertSame(cleaner, appender.logCleaner)
    }

    @Test
    fun `default log cleaner deletes expired events between appends`() {
        appender.maxHistory = "1 hour"
        appender.start()

        appender.doAppend(event("expired"))
        now += 40 * MINUTE_MS
        appender.doAppend(event("kept"))
        // an hour since the startup cleanup: deletes events up to 1 hour old
        now += 40 * MINUTE_MS
        appender.doAppend(event("latest"))

        assertEquals(listOf("kept", "latest"), messages())
    }

    @Test
    fun `default log cleaner deletes expired events at startup from custom tables`() {
        appender.dbNameResolver = SimpleDBNameResolver().apply { setTableNamePrefix("app_") }
        appender.start()
        appender.doAppend(event("expired"))
        now += 40 * MINUTE_MS
        appender.doAppend(event("kept"))
        appender.stop()

        now += 40 * MINUTE_MS
        appender.maxHistory = "1 hour"
        appender.start()

        assertEquals(listOf("kept"), messages(table = "app_logging_event"))
        assertEquals(emptyList(), problems())
    }

    // --- append -------------------------------------------------------------

    @Test
    fun `append stores the event details`() {
        appender.start()

        appender.doAppend(event("hello", level = Level.WARN))

        val row = loggedEvents().single()
        assertEquals(
            mapOf(
                "event_id" to "1",
                "timestmp" to START_MS.toString(),
                "formatted_message" to "hello",
                "logger_name" to LOGGER_NAME,
                "level_string" to "WARN",
                "thread_name" to THREAD_NAME,
                "reference_flag" to "0",
                "arg0" to null,
                "arg1" to null,
                "arg2" to null,
                "arg3" to null,
                "caller_filename" to "Caller.java",
                "caller_class" to "com.example.Caller",
                "caller_method" to "doWork",
                "caller_line" to "42",
            ),
            row,
        )
        assertEquals(emptyList(), properties())
        assertEquals(emptyList(), exceptionLines())
        assertEquals(emptyList(), problems())
    }

    @Test
    fun `append stores the caller of the logging statement`() {
        appender.start()
        val logger = context.getLogger("caller")
        logger.addAppender(appender)

        val here = Throwable().stackTrace[0]
        logger.info("where am I?") // must stay on the line after `here`

        val row = loggedEvents().single()
        assertEquals(here.fileName, row["caller_filename"])
        assertEquals(here.className, row["caller_class"])
        assertEquals(here.methodName, row["caller_method"])
        assertEquals((here.lineNumber + 1).toString(), row["caller_line"])
    }

    @Test
    fun `append stores the first four arguments`() {
        appender.start()

        appender.doAppend(event("{} {} {} {} {}", args = arrayOf("one", 2, 3.5, 'c', "five")))

        val row = loggedEvents().single()
        assertEquals("one 2 3.5 c five", row["formatted_message"])
        assertEquals(listOf("one", "2", "3.5", "c"), row.args())
    }

    @Test
    fun `append leaves the columns of missing arguments null`() {
        appender.start()

        appender.doAppend(event("{} {}", args = arrayOf("one", "two")))

        assertEquals(listOf("one", "two", null, null), loggedEvents().single().args())
    }

    @Test
    fun `append stores null arguments as empty strings`() {
        appender.start()
        val nullString = mock<Any>()
        doReturn(null).whenever(nullString).toString()

        appender.doAppend(event("{} {}", args = arrayOf(null, nullString)))

        val row = loggedEvents().single()
        assertEquals("null null", row["formatted_message"])
        assertEquals(listOf("", "", null, null), row.args())
    }

    @Test
    fun `append truncates arguments to 254 characters`() {
        appender.start()
        val long = "x".repeat(300)
        val limit = "y".repeat(254)

        appender.doAppend(event("{} {}", args = arrayOf(long, limit)))

        val row = loggedEvents().single()
        assertEquals("$long $limit", row["formatted_message"])
        assertEquals(listOf("x".repeat(254), limit, null, null), row.args())
    }

    @Test
    fun `append stores MDC and context properties with MDC taking priority`() {
        appender.start()
        context.putProperty("app", "demo")
        context.putProperty("shared", "from context")

        appender.doAppend(event(mdc = mapOf("user" to "alice", "shared" to "from MDC")))

        assertEquals("1", loggedEvents().single()["reference_flag"])
        assertEquals(
            listOf(
                mapOf("event_id" to "1", "mapped_key" to "app", "mapped_value" to "demo"),
                mapOf("event_id" to "1", "mapped_key" to "shared", "mapped_value" to "from MDC"),
                mapOf("event_id" to "1", "mapped_key" to "user", "mapped_value" to "alice"),
            ),
            properties(),
        )
    }

    @Test
    fun `append stores context properties when the MDC is empty`() {
        appender.start()
        context.putProperty("app", "demo")

        appender.doAppend(event())

        assertEquals("1", loggedEvents().single()["reference_flag"])
        assertEquals(listOf(mapOf("event_id" to "1", "mapped_key" to "app", "mapped_value" to "demo")), properties())
    }

    @Test
    fun `append flags the properties and exception of each event and links them to it`() {
        appender.start()

        appender.doAppend(event("plain"))
        appender.doAppend(event("with MDC", mdc = mapOf("user" to "alice")))
        appender.doAppend(event("with exception", throwable = RuntimeException("boom")))
        appender.doAppend(event("with both", mdc = mapOf("user" to "bob"), throwable = IllegalStateException("bang")))

        // reference_flag bits: 1 = has properties, 2 = has an exception
        assertEquals(
            listOf(
                listOf("1", "plain", "0"),
                listOf("2", "with MDC", "1"),
                listOf("3", "with exception", "2"),
                listOf("4", "with both", "3"),
            ),
            loggedEvents().map { listOf(it["event_id"], it["formatted_message"], it["reference_flag"]) },
        )
        assertEquals(listOf("2" to "alice", "4" to "bob"), properties().map { it["event_id"] to it["mapped_value"] })
        assertEquals(
            listOf("3" to "java.lang.RuntimeException: boom", "4" to "java.lang.IllegalStateException: bang"),
            exceptionLines().filter { it["i"] == "0" }.map { it["event_id"] to it["trace_line"] },
        )
    }

    @Test
    fun `append stores events without MDC or context properties or caller data`() {
        appender.start()
        val event = event("remote").apply { setLoggerContextRemoteView(LoggerContextVO("remote", null, 0)) }

        appender.doAppend(BareEvent(event))

        val row = loggedEvents().single()
        assertEquals("remote", row["formatted_message"])
        assertEquals("0", row["reference_flag"])
        assertEquals(listOf(null, null, null, null), row.callerColumns())
        assertEquals(emptyList(), properties())
        assertEquals(emptyList(), problems())
    }

    @Test
    fun `append stores empty caller data as nulls`() {
        appender.start()

        appender.doAppend(event("no frames").apply { callerData = emptyArray() })
        appender.doAppend(event("null frame").apply { callerData = arrayOf(null) })

        val rows = loggedEvents()
        assertEquals(listOf("no frames", "null frame"), rows.map { it["formatted_message"] })
        rows.forEach { assertEquals(listOf(null, null, null, null), it.callerColumns()) }
    }

    @Test
    fun `append stores a null caller file name as null`() {
        appender.start()
        val caller = StackTraceElement("com.example.Generated", "invoke", null, -2)

        appender.doAppend(event().apply { callerData = arrayOf(caller) })

        assertEquals(listOf(null, "com.example.Generated", "invoke", "-2"), loggedEvents().single().callerColumns())
    }

    @Test
    fun `append stores the exception with its causes and common frames`() {
        appender.start()
        val cause = IllegalStateException("inner")
        val error = RuntimeException("outer", cause)

        appender.doAppend(event("failed", throwable = error))

        assertEquals("2", loggedEvents().single()["reference_flag"])
        val expectedLines = buildList {
            add("java.lang.RuntimeException: outer")
            error.stackTrace.forEach { add("\tat $it") }
            // the cause was created one line above the error; its other frames are shared
            add("Caused by: java.lang.IllegalStateException: inner")
            add("\tat ${cause.stackTrace[0]}")
            add("\t... ${cause.stackTrace.size - 1} common frames omitted")
        }
        val lines = exceptionLines()
        assertEquals(expectedLines, lines.map { it["trace_line"] })
        assertEquals(expectedLines.indices.map { it.toString() }, lines.map { it["i"] })
        assertTrue(lines.all { it["event_id"] == "1" })
    }

    @Test
    fun `append warns and drops an event the database rejects`() {
        appender.start()
        execSql(
            "CREATE TRIGGER reject_event BEFORE INSERT ON logging_event WHEN NEW.formatted_message = 'rejected' " +
                "BEGIN SELECT RAISE(ABORT, 'event rejected'); END"
        )

        appender.doAppend(event("rejected", throwable = RuntimeException("boom"), mdc = mapOf("user" to "alice")))
        appender.doAppend(event("accepted"))

        assertIs<SQLiteConstraintException>(assertStatus(Status.WARN, "Failed to insert loggingEvent").throwable)
        assertEquals(listOf("Failed to insert loggingEvent"), problems())
        assertEquals(listOf("accepted"), messages())
        assertEquals(emptyList(), properties())
        assertEquals(emptyList(), exceptionLines())
    }

    @Test
    fun `append rolls back an event whose properties the database rejects`() {
        appender.start()
        execSql(
            "CREATE TRIGGER reject_property BEFORE INSERT ON logging_event_property " +
                "BEGIN SELECT RAISE(ABORT, 'property rejected'); END"
        )

        appender.doAppend(event("with MDC", mdc = mapOf("user" to "alice")))
        appender.doAppend(event("without MDC"))

        assertIs<SQLiteConstraintException>(assertStatus(Status.ERROR, "Cannot append event").throwable)
        assertEquals(listOf("without MDC"), messages())
        assertEquals(emptyList(), properties())
    }

    @Test
    fun `append rolls back an event whose exception the database rejects`() {
        appender.start()
        execSql(
            "CREATE TRIGGER reject_exception BEFORE INSERT ON logging_event_exception " +
                "BEGIN SELECT RAISE(ABORT, 'exception rejected'); END"
        )

        appender.doAppend(event("with exception", throwable = RuntimeException("boom")))
        appender.doAppend(event("without exception"))

        assertIs<SQLiteConstraintException>(assertStatus(Status.ERROR, "Cannot append event").throwable)
        assertEquals(listOf("without exception"), messages())
        assertEquals(emptyList(), exceptionLines())
    }

    @Test
    fun `append closes every statement it compiles`() {
        val db = mockDatabase(inTransaction = true)
        startWithDatabase(db)

        appender.doAppend(event(mdc = mapOf("user" to "alice"), throwable = RuntimeException("boom")))

        assertEquals(emptyList(), problems())
        // one statement each for the event, its properties and its exception
        assertEquals(3, compiledStatements.size)
        compiledStatements.forEach { verify(it).close() }
        verify(db).setTransactionSuccessful()
        verify(db).endTransaction()
    }

    @Test
    fun `append reports a transaction that fails to begin without ending it`() {
        val db = mockDatabase(inTransaction = false)
        doThrow(SQLiteDatabaseLockedException("database is locked")).whenever(db).beginTransaction()
        startWithDatabase(db)

        appender.doAppend(event())

        // the original error is reported, not one from ending a transaction that never began
        assertIs<SQLiteDatabaseLockedException>(assertStatus(Status.ERROR, "Cannot append event").throwable)
        val statement = compiledStatements.single()
        verify(statement, never()).executeInsert()
        verify(statement).close()
        verify(db, never()).endTransaction()
    }

    @Test
    fun `append does not end a transaction that is no longer active`() {
        // a real database always has an active transaction here; only a mock can end it early
        val db = mockDatabase(inTransaction = false)
        startWithDatabase(db)

        appender.doAppend(event())

        assertEquals(emptyList(), problems())
        val statement = compiledStatements.single()
        verify(statement).executeInsert()
        verify(statement).close()
        verify(db).setTransactionSuccessful()
        verify(db, never()).endTransaction()
    }

    @Test
    fun `append does nothing before start`() {
        appender.append(event())

        assertFalse(dbFile.exists())
        assertEquals(emptyList(), context.statusManager.copyOfStatusList)
    }

    // --- stop -----------------------------------------------------------------

    @Test
    fun `stop before start does nothing`() {
        appender.stop()

        assertFalse(appender.isStarted)
        assertEquals(emptyList(), context.statusManager.copyOfStatusList)
    }

    @Test
    fun `stop closes the database`() {
        appender.start()
        appender.doAppend(event("before stop"))

        appender.stop()

        assertAppendFailsOnClosedDatabase()
        assertEquals(listOf("before stop"), messages())
    }

    // --- helpers ------------------------------------------------------------

    private fun newAppender() = SQLiteAppender().also {
        it.setClock(Clock { now })
        it.filename = dbFile.absolutePath
        it.context = context
    }

    /**
     * A mock database that compiles each SQL statement into a new mock statement
     * (recorded in [compiledStatements]) that inserts row 1. When it is not
     * [inTransaction], endTransaction() fails as the real one does.
     */
    private fun mockDatabase(inTransaction: Boolean) = mock<SQLiteDatabase>().also { db ->
        whenever(db.compileStatement(any())).thenAnswer {
            mock<SQLiteStatement> { on { executeInsert() } doReturn 1L }.also { compiledStatements += it }
        }
        whenever(db.inTransaction()).thenReturn(inTransaction)
        if (!inTransaction) {
            doThrow(IllegalStateException("Cannot perform this operation because there is no current transaction."))
                .whenever(db).endTransaction()
        }
    }

    /** Starts the appender with [db] as the database it opens */
    private fun startWithDatabase(db: SQLiteDatabase) {
        mockStatic(SQLiteDatabase::class.java).use { sqlite ->
            sqlite.`when`<SQLiteDatabase> {
                SQLiteDatabase.openOrCreateDatabase(any<String>(), anyOrNull<SQLiteDatabase.CursorFactory>())
            }.thenReturn(db)
            appender.start()
        }
        assertTrue(appender.isStarted)
    }

    private fun startWithMockCleaner(maxHistory: String?) {
        maxHistory?.let { appender.maxHistory = it }
        appender.logCleaner = logCleaner
        appender.start()
    }

    private fun appendEvents(count: Int, stepMs: Long) {
        repeat(count) {
            appender.doAppend(event("i=$it"))
            now += stepMs
        }
    }

    private fun event(
        message: String = "message",
        args: Array<Any?>? = null,
        throwable: Throwable? = null,
        mdc: Map<String, String> = emptyMap(),
        level: Level = Level.INFO,
    ) = LoggingEvent(Logger::class.java.name, context.getLogger(LOGGER_NAME), level, message, throwable, args).apply {
        timeStamp = now
        threadName = THREAD_NAME
        mdcPropertyMap = mdc
        callerData = arrayOf(StackTraceElement("com.example.Caller", "doWork", "Caller.java", 42))
    }

    private fun configureFromXml(appenderXml: String): SQLiteAppender {
        val xml = """
            <configuration>
              <appender name="sqlite" class="${SQLiteAppender::class.java.name}">
                <filename>${dbFile.absolutePath}</filename>
                $appenderXml
              </appender>
              <root level="DEBUG"><appender-ref ref="sqlite"/></root>
            </configuration>
        """
        JoranConfigurator().also { it.context = context }.doConfigure(xml.byteInputStream())
        return context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("sqlite") as SQLiteAppender
    }

    private fun defaultDatabaseFile() =
        File(RuntimeEnvironment.getApplication().getDatabasePath("logback.db").absolutePath)

    private fun assertStatus(level: Int, message: String): Status {
        val statuses = context.statusManager.copyOfStatusList
        return assertNotNull(
            statuses.singleOrNull { it.level == level && it.message == message },
            "expected a single status \"$message\" in $statuses",
        )
    }

    /**
     * Appends an event after the appender closed its database. stop() does not
     * clear isStarted, so the event reaches the closed database.
     */
    private fun assertAppendFailsOnClosedDatabase() {
        appender.doAppend(event("after close"))

        assertIs<IllegalStateException>(assertStatus(Status.ERROR, "Cannot append event").throwable)
        assertFalse("after close" in messages())
    }

    /** Messages of the warnings and errors reported so far */
    private fun problems() = context.statusManager.copyOfStatusList.filter { it.level >= Status.WARN }.map { it.message }

    private fun execSql(sql: String) =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.execSQL(sql) }

    /** Runs [sql] on a separate connection, returning each row as a map of column name to value */
    private fun query(sql: String): List<Map<String, String?>> =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery(sql, null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.columnNames.associateWith { cursor.getString(cursor.getColumnIndexOrThrow(it)) })
                    }
                }
            }
        }

    private fun rows(table: String, orderBy: String) = query("SELECT * FROM $table ORDER BY $orderBy")

    private fun tableNames() =
        query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT IN ('android_metadata', 'sqlite_sequence')")
            .map { it["name"] }
            .toSet()

    private fun loggedEvents() = rows("logging_event", "event_id")

    private fun messages(table: String = "logging_event") = rows(table, "event_id").map { it["formatted_message"] }

    private fun properties() = rows("logging_event_property", "event_id, mapped_key")

    private fun exceptionLines() = rows("logging_event_exception", "event_id, i")

    private fun Map<String, String?>.args() = listOf(this["arg0"], this["arg1"], this["arg2"], this["arg3"])

    private fun Map<String, String?>.callerColumns() =
        listOf(this["caller_filename"], this["caller_class"], this["caller_method"], this["caller_line"])

    /** An [ILoggingEvent] implementation that provides neither an MDC map nor caller data */
    private class BareEvent(event: ILoggingEvent) : ILoggingEvent by event {
        override fun getMDCPropertyMap(): Map<String, String>? = null
        override fun getCallerData(): Array<StackTraceElement>? = null
    }

    private companion object {
        const val START_MS = 1_700_000_000_000L
        const val MINUTE_MS = 60 * 1000L
        const val HOUR_MS = 60 * MINUTE_MS
        const val EXPIRY_MS = 500L
        const val LOGGER_NAME = "ch.qos.logback.classic.android.SQLiteAppenderTest"
        const val THREAD_NAME = "test-thread"
    }
}
