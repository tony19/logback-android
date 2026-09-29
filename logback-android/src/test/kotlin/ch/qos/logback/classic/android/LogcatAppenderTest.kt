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

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.status.Status
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the [LogcatAppender] class
 *
 * @author Anthony Trinh
 */
@RunWith(RobolectricTestRunner::class)
class LogcatAppenderTest {

    private val context = LoggerContext()
    private val root: Logger = context.getLogger(Logger.ROOT_LOGGER_NAME)
    private val xmlContexts = mutableListOf<LoggerContext>()
    private lateinit var logcatAppender: TagExposingAppender

    @Before
    fun before() {
        context.reset()
        root.detachAndStopAllAppenders()
        ShadowLog.reset()
        logcatAppender = configureLogcatAppender()
    }

    @After
    fun after() {
        logcatAppender.stop()
        root.detachAndStopAllAppenders()
        context.stop()
        xmlContexts.forEach { it.stop() }
        ShadowLog.reset()
    }

    // --- tag length ---------------------------------------------------------

    @Test
    fun `long tag allowed if not checkLoggable`() {
        setTagPattern(TAG, checkLoggable = false)

        assertEquals(TAG, logcatAppender.tagOf(event(message = TAG)))
    }

    @Test
    fun `long tag truncated if checkLoggable`() {
        setTagPattern(TAG, checkLoggable = true)

        assertEquals(TRUNCATED_TAG, logcatAppender.tagOf(event(message = TAG)))
    }

    @Test
    fun `tag of max length is not truncated if checkLoggable`() {
        val maxLengthTag = TAG.substring(0, MAX_TAG_LENGTH)
        setTagPattern(maxLengthTag, checkLoggable = true)

        assertEquals(maxLengthTag, logcatAppender.tagOf(event()))
    }

    @Test
    fun `tag one char over max length is truncated if checkLoggable`() {
        setTagPattern(TAG.substring(0, MAX_TAG_LENGTH + 1), checkLoggable = true)

        assertEquals(TRUNCATED_TAG, logcatAppender.tagOf(event()))
    }

    @Test
    fun `tag defaults to logger name without tag encoder`() {
        val appender = startedAppender(TagExposingAppender(), checkLoggable = true)

        assertEquals("short.Name", appender.tagOf(event(loggerName = "short.Name")))
        assertEquals(TRUNCATED_TAG, appender.tagOf(event(loggerName = TAG)))
    }

    @Test
    fun `checkLoggable asks logcat about the truncated tag it logs with`() {
        val appender = startedAppender(checkLoggable = true)
        // only the truncated tag is restricted; the full logger name keeps the default INFO threshold
        ShadowLog.setLoggable(TRUNCATED_TAG, Log.ERROR)

        appender.doAppend(event(Level.WARN, "warn", loggerName = TAG))
        appender.doAppend(event(Level.ERROR, "error", loggerName = TAG))

        assertEquals(listOf(Triple(Log.ERROR, TRUNCATED_TAG, "error")), logcat())
    }

    @Test
    fun `checkLoggable asks logcat about the tag from the tag encoder`() {
        val appender = startedAppender(tagEncoder = patternEncoder("%logger{0}"), checkLoggable = true)
        // only the tag is restricted; the logger name keeps the default INFO threshold
        ShadowLog.setLoggable("Bar", Log.ERROR)

        appender.doAppend(event(Level.WARN, "warn", loggerName = "foo.Bar"))
        appender.doAppend(event(Level.ERROR, "error", loggerName = "foo.Bar"))

        assertEquals(listOf(Triple(Log.ERROR, "Bar", "error")), logcat())
    }

    @Test
    fun `long logger name reaches logcat untruncated if not checkLoggable`() {
        val appender = startedAppender()

        appender.doAppend(event(Level.WARN, "hello", loggerName = TAG))

        assertEquals(listOf(Triple(Log.WARN, TAG, "hello")), logcat())
    }

    // --- stack traces in tags (issue #34) ------------------------------------

    // Issue #34
    @Test
    fun `tag excludes stack traces`() {
        // create logging event with throwable
        val event = event(message = TAG, throwable = Throwable("throwable"))

        setTagPattern(TAG, checkLoggable = true)

        // if the tags match, it does not include the stack trace
        val actualTag = assertNotNull(logcatAppender.tagEncoder).layout.doLayout(event)
        assertEquals(TAG, actualTag)
    }

    @Test
    fun `start appends nopex to tag pattern only once`() {
        val tagEncoder = patternEncoder("%logger")
        val appender = startedAppender(tagEncoder = tagEncoder)
        assertEquals("%logger%nopex", tagEncoder.pattern)

        // restarting must not append a second %nopex
        appender.stop()
        appender.start()
        assertTrue(appender.isStarted)
        assertEquals("%logger%nopex", tagEncoder.pattern)

        appender.doAppend(event(message = "hello", throwable = Throwable("boom")))
        assertEquals(LOGGER_NAME, logcat().single().second)
    }

    @Test
    fun `tag pattern already containing nopex is kept as is`() {
        val tagEncoder = patternEncoder("%logger{0}%nopex")
        val appender = startedAppender(tagEncoder = tagEncoder)

        assertEquals("%logger{0}%nopex", tagEncoder.pattern)
        appender.doAppend(event(message = "hello", loggerName = "a.b.Tag", throwable = Throwable("boom")))
        assertEquals("Tag", logcat().single().second)
    }

    @Test
    fun `tag PatternLayout wrapped by LayoutWrappingEncoder gets nopex`() {
        val tagLayout = patternLayout("%logger")
        val appender = startedAppender(tagEncoder = layoutEncoder(tagLayout))

        assertEquals("%logger%nopex", tagLayout.pattern)
        assertTrue(tagLayout.isStarted)
        appender.doAppend(event(message = "hello", throwable = Throwable("boom")))
        assertEquals(LOGGER_NAME, logcat().single().second)
    }

    @Test
    fun `wrapped tag PatternLayout already containing nopex is kept as is`() {
        val tagLayout = patternLayout("[%logger]%nopex")
        val appender = startedAppender(tagEncoder = layoutEncoder(tagLayout))

        assertEquals("[%logger]%nopex", tagLayout.pattern)
        appender.doAppend(event(message = "hello", throwable = Throwable("boom")))
        assertEquals("[$LOGGER_NAME]", logcat().single().second)
    }

    @Test
    fun `wrapped tag PatternLayout without pattern is left alone`() {
        // a PatternLayout without a pattern fails to start and lays out nothing
        val tagLayout = PatternLayout().apply { context = this@LogcatAppenderTest.context }
        val appender = startedAppender(tagEncoder = layoutEncoder(tagLayout))

        assertTrue(appender.isStarted)
        assertNull(tagLayout.pattern)
        assertFalse(tagLayout.isStarted)
        appender.doAppend(event(Level.INFO, "hello"))
        assertEquals(listOf(Triple(Log.INFO, "", "hello")), logcat())
    }

    @Test
    fun `tag layout that is not a PatternLayout is used as is`() {
        val tagLayout = FixedLayout("custom-tag")
        val appender = startedAppender(tagEncoder = layoutEncoder(tagLayout))

        assertTrue(appender.isStarted)
        appender.doAppend(event(Level.INFO, "hello"))
        assertEquals(listOf(Triple(Log.INFO, "custom-tag", "hello")), logcat())
    }

    // --- getTag() edge cases ----------------------------------------------------

    @Test
    fun `null tag from tag layout falls back to the logger name`() {
        val appender = startedAppender(tagEncoder = layoutEncoder(FixedLayout(null)))

        appender.doAppend(event(Level.INFO, "hello"))

        assertEquals(listOf(Triple(Log.INFO, LOGGER_NAME, "hello")), logcat())
        assertEquals(emptyList(), errorMessages())
    }

    @Test
    fun `event without logger name is logged with null tag`() {
        val appender = startedAppender()

        appender.doAppend(event(Level.INFO, "hello", loggerName = null))

        assertEquals(listOf(Triple(Log.INFO, null, "hello")), logcat())
        assertEquals(emptyList(), errorMessages())
    }

    @Test
    fun `event without logger name is logged with null tag if checkLoggable`() {
        val appender = startedAppender(checkLoggable = true)

        appender.doAppend(event(Level.INFO, "hello", loggerName = null))

        assertEquals(listOf(Triple(Log.INFO, null, "hello")), logcat())
        assertEquals(emptyList(), errorMessages())
    }

    @Test
    fun `tag encoder without layout set after start falls back to the logger name`() {
        // start() validates the tag encoder, but setting one later bypasses that
        val appender = startedAppender()
        appender.tagEncoder = LayoutWrappingEncoder()

        appender.doAppend(event(Level.INFO, "hello"))

        assertEquals(listOf(Triple(Log.INFO, LOGGER_NAME, "hello")), logcat())
        assertEquals(emptyList(), errorMessages())
    }

    // --- properties and start() validation --------------------------------------

    @Test
    fun `defaults to no encoders and no loggable check`() {
        val appender = LogcatAppender()

        assertNull(appender.encoder)
        assertNull(appender.tagEncoder)
        assertFalse(appender.checkLoggable)
        assertFalse(appender.isStarted)
    }

    @Test
    fun `exposes configured encoders and loggable check`() {
        val encoder = patternEncoder("%msg")
        val tagEncoder = patternEncoder("%logger")
        val appender = LogcatAppender().apply {
            this.encoder = encoder
            this.tagEncoder = tagEncoder
            checkLoggable = true
        }

        assertSame(encoder, appender.encoder)
        assertSame(tagEncoder, appender.tagEncoder)
        assertTrue(appender.checkLoggable)
    }

    @Test
    fun `start fails without encoder`() {
        val appender = LogcatAppender().apply {
            context = this@LogcatAppenderTest.context
            name = "no-encoder"
        }

        appender.start()

        assertFalse(appender.isStarted)
        assertEquals(listOf("No layout set for the appender named [no-encoder]."), errorMessages())
    }

    @Test
    fun `start fails when encoder has no layout`() {
        val appender = LogcatAppender().apply {
            context = this@LogcatAppenderTest.context
            name = "no-layout"
            encoder = LayoutWrappingEncoder()
        }

        appender.start()

        assertFalse(appender.isStarted)
        assertEquals(listOf("No layout set for the appender named [no-layout]."), errorMessages())
    }

    @Test
    fun `start fails when tag encoder has no layout`() {
        val appender = LogcatAppender().apply {
            context = this@LogcatAppenderTest.context
            name = "no-tag-layout"
            encoder = patternEncoder("%msg")
            tagEncoder = LayoutWrappingEncoder()
        }

        appender.start()

        assertFalse(appender.isStarted)
        assertEquals(listOf("No tag layout set for the appender named [no-tag-layout]."), errorMessages())
    }

    @Test
    fun `start succeeds without tag encoder`() {
        val appender = startedAppender()

        assertTrue(appender.isStarted)
        assertEquals(emptyList(), errorMessages())
    }

    // --- append() -------------------------------------------------------------

    @Test
    fun `does not log if not started`() {
        val appender = LogcatAppender().apply {
            context = this@LogcatAppenderTest.context
            encoder = patternEncoder("%msg")
        }

        appender.append(event(Level.ERROR, "hello"))

        assertEquals(emptyList(), logcat())
    }

    @Test
    fun `logs each level at its logcat priority`() {
        val appender = startedAppender()

        appender.appendAtEveryLevel()

        val expected = PRIORITY_BY_LEVEL.map { (level, priority) -> Triple(priority, LOGGER_NAME, "message at $level") }
        assertEquals(expected, logcat())
    }

    @Test
    fun `logs every level regardless of logcat filter if not checkLoggable`() {
        val appender = startedAppender()
        ShadowLog.setLoggable(LOGGER_NAME, Log.ASSERT)

        appender.appendAtEveryLevel()

        assertEquals(PRIORITY_BY_LEVEL.values.toList(), logcat().map { it.first })
    }

    @Test
    fun `checkLoggable logs only the levels logcat allows for the tag`() {
        val appender = startedAppender(checkLoggable = true)

        for (threshold in Log.VERBOSE..Log.ASSERT) {
            ShadowLog.reset()
            ShadowLog.setLoggable(LOGGER_NAME, threshold)

            appender.appendAtEveryLevel()

            val expected = PRIORITY_BY_LEVEL
                .filterValues { it >= threshold }
                .map { (level, priority) -> Triple(priority, LOGGER_NAME, "message at $level") }
            assertEquals(expected, logcat(), "logcat threshold $threshold")
        }
    }

    @Test
    fun `checkLoggable follows logcat default threshold of INFO`() {
        val appender = startedAppender(checkLoggable = true)

        appender.appendAtEveryLevel()

        assertEquals(listOf(Log.INFO, Log.WARN, Log.ERROR), logcat().map { it.first })
    }

    @Test
    fun `never logs OFF or unknown levels`() {
        // without checkLoggable, any level that reached a Log call would show up in logcat
        val appender = startedAppender()

        appender.doAppend(event(Level.OFF, "off"))
        appender.doAppend(event(CUSTOM_LEVEL, "custom"))

        assertEquals(emptyList(), logcat())
        assertEquals(emptyList(), errorMessages())
    }

    @Test
    fun `message is formatted by the encoder layout`() {
        val appender = startedAppender(encoder = patternEncoder("%level: %msg"))

        appender.doAppend(event(Level.DEBUG, "hello"))

        assertEquals(listOf(Triple(Log.DEBUG, LOGGER_NAME, "DEBUG: hello")), logcat())
    }

    // Issue #102
    @Test
    fun `logs exception when message trails with newline`() {
        addLogcatAppenderToRoot()
        ShadowLog.reset()
        context.getLogger(LOGGER_NAME).debug("msg\n", NullPointerException())
        assertLogcatContains(Log.DEBUG, NullPointerException::class.java.name)
    }

    // Issue #102
    @Test
    fun `logs exception when message has no trailing newline`() {
        addLogcatAppenderToRoot()
        ShadowLog.reset()
        context.getLogger(LOGGER_NAME).debug("msg", NullPointerException())
        assertLogcatContains(Log.DEBUG, NullPointerException::class.java.name)
    }

    // --- Joran configuration (issue #376) ----------------------------------------

    // Issue #376
    @Test
    fun `supports LayoutWrappingEncoder`() {
        val ctx = configureFromXml(
            """
            <configuration>
              <appender name='logcat' class='ch.qos.logback.classic.android.LogcatAppender'>
                <encoder class='ch.qos.logback.core.encoder.LayoutWrappingEncoder'>
                  <layout class='ch.qos.logback.classic.PatternLayout'>
                    <pattern>wrapped: %msg</pattern>
                  </layout>
                </encoder>
              </appender>
              <root level='DEBUG'><appender-ref ref='logcat'/></root>
            </configuration>
            """
        )

        val appender = ctx.logcatAppender()
        assertTrue(appender.isStarted)
        val encoder = assertNotNull(appender.encoder)
        assertEquals<Class<*>>(LayoutWrappingEncoder::class.java, encoder.javaClass)
        assertEquals("wrapped: hello", encoder.layout.doLayout(event(message = "hello")))

        ctx.getLogger("foo.Bar").info("hello")
        assertEquals(listOf(Triple(Log.INFO, "foo.Bar", "wrapped: hello")), logcat())
    }

    // Issue #376: an encoder element without a class attribute must still
    // default to PatternLayoutEncoder
    @Test
    fun `default encoder type is PatternLayoutEncoder`() {
        val ctx = configureFromXml(
            """
            <configuration>
              <appender name='logcat' class='ch.qos.logback.classic.android.LogcatAppender'>
                <encoder><pattern>%msg</pattern></encoder>
                <tagEncoder><pattern>tag</pattern></tagEncoder>
              </appender>
              <root level='DEBUG'><appender-ref ref='logcat'/></root>
            </configuration>
            """
        )

        val appender = ctx.logcatAppender()
        assertTrue(appender.isStarted)
        assertIs<PatternLayoutEncoder>(appender.encoder)
        assertIs<PatternLayoutEncoder>(appender.tagEncoder)

        ctx.getLogger("foo.Bar").debug("hello")
        assertEquals(listOf(Triple(Log.DEBUG, "tag", "hello")), logcat())
    }

    @Test
    fun `checkLoggable is configurable from XML`() {
        val ctx = configureFromXml(
            """
            <configuration>
              <appender name='logcat' class='ch.qos.logback.classic.android.LogcatAppender'>
                <checkLoggable>true</checkLoggable>
                <encoder><pattern>%msg</pattern></encoder>
              </appender>
              <root level='DEBUG'><appender-ref ref='logcat'/></root>
            </configuration>
            """
        )

        assertTrue(ctx.logcatAppender().checkLoggable)
        // logcat's default threshold (INFO) now applies, although the root logger allows DEBUG
        ctx.getLogger("foo.Bar").debug("dropped")
        ctx.getLogger("foo.Bar").info("kept")
        assertEquals(listOf(Triple(Log.INFO, "foo.Bar", "kept")), logcat())
    }

    // --- helpers ------------------------------------------------------------------

    /** Exposes the protected [LogcatAppender.getTag] to the tests. */
    private class TagExposingAppender : LogcatAppender() {
        fun tagOf(event: ILoggingEvent): String? = getTag(event)
    }

    /** A layout that always produces the same text (or `null`). */
    private class FixedLayout(private val text: String?) : LayoutBase<ILoggingEvent>() {
        override fun doLayout(event: ILoggingEvent): String? = text
    }

    private fun setTagPattern(tag: String, checkLoggable: Boolean) {
        logcatAppender.stop()
        logcatAppender.checkLoggable = checkLoggable
        (logcatAppender.tagEncoder as PatternLayoutEncoder).pattern = tag
        logcatAppender.start()
    }

    private fun configureLogcatAppender(): TagExposingAppender =
        TagExposingAppender().apply {
            context = this@LogcatAppenderTest.context
            name = LOGGER_NAME
            tagEncoder = patternEncoder(TAG)
            encoder = patternEncoder("%msg")
            start()
        }

    private fun addLogcatAppenderToRoot() {
        val appender = startedAppender(encoder = patternEncoder("[%thread] %method\\(\\): %msg%n"))
        root.addAppender(appender)
    }

    private fun assertLogcatContains(level: Int, errorMessage: String) {
        val logs = assertNotNull(ShadowLog.getLogsForTag(LOGGER_NAME))
        assertTrue(logs.any { it.type == level && it.msg.contains(errorMessage) }, "logcat: $logs")
    }

    private fun <T : LogcatAppender> startedAppender(
        appender: T,
        encoder: LayoutWrappingEncoder<ILoggingEvent> = patternEncoder("%msg"),
        tagEncoder: LayoutWrappingEncoder<ILoggingEvent>? = null,
        checkLoggable: Boolean = false,
    ): T = appender.apply {
        context = this@LogcatAppenderTest.context
        name = LOGGER_NAME
        this.encoder = encoder
        this.tagEncoder = tagEncoder
        this.checkLoggable = checkLoggable
        start()
    }

    private fun startedAppender(
        encoder: LayoutWrappingEncoder<ILoggingEvent> = patternEncoder("%msg"),
        tagEncoder: LayoutWrappingEncoder<ILoggingEvent>? = null,
        checkLoggable: Boolean = false,
    ): LogcatAppender = startedAppender(LogcatAppender(), encoder, tagEncoder, checkLoggable)

    private fun patternEncoder(pattern: String): PatternLayoutEncoder =
        PatternLayoutEncoder().apply {
            context = this@LogcatAppenderTest.context
            this.pattern = pattern
            start()
        }

    private fun patternLayout(pattern: String): PatternLayout =
        PatternLayout().apply {
            context = this@LogcatAppenderTest.context
            this.pattern = pattern
            start()
        }

    private fun layoutEncoder(layout: LayoutBase<ILoggingEvent>): LayoutWrappingEncoder<ILoggingEvent> =
        LayoutWrappingEncoder<ILoggingEvent>().apply {
            context = this@LogcatAppenderTest.context
            this.layout = layout
            start()
        }

    private fun event(
        level: Level = Level.INFO,
        message: String = "message",
        loggerName: String? = LOGGER_NAME,
        throwable: Throwable? = null,
    ): LoggingEvent = LoggingEvent().apply {
        this.loggerName = loggerName
        this.level = level
        this.message = message
        if (throwable != null) setThrowableProxy(ThrowableProxy(throwable))
    }

    /** Appends one "message at <level>" event for each level in [PRIORITY_BY_LEVEL]. */
    private fun LogcatAppender.appendAtEveryLevel() =
        PRIORITY_BY_LEVEL.keys.forEach { doAppend(event(it, "message at $it")) }

    /** Everything written to logcat so far, as (priority, tag, message). */
    private fun logcat(): List<Triple<Int, String?, String?>> =
        ShadowLog.getLogs().map { Triple(it.type, it.tag, it.msg) }

    private fun errorMessages(lc: LoggerContext = context): List<String> =
        lc.statusManager.copyOfStatusList
            .filter { it.level == Status.ERROR }
            .map { it.message }

    /** Configures a new context from [config], which must configure without errors. */
    private fun configureFromXml(config: String): LoggerContext {
        val ctx = LoggerContext().also { xmlContexts += it }
        JoranConfigurator().apply {
            context = ctx
            doConfigure(ByteArrayInputStream(config.toByteArray()))
        }
        assertEquals(emptyList(), errorMessages(ctx), "configuration errors")
        return ctx
    }

    private fun LoggerContext.logcatAppender(): LogcatAppender =
        assertIs(getLogger(Logger.ROOT_LOGGER_NAME).getAppender("logcat"))

    private companion object {
        const val LOGGER_NAME = "LOGCAT"
        const val MAX_TAG_LENGTH = 23 // for android.util.Log.isLoggable()
        const val TAG = "123456789012345678901234567890"
        val TRUNCATED_TAG = TAG.substring(0, MAX_TAG_LENGTH - 1) + "*"

        /** The logcat priority each logback level is written at. */
        val PRIORITY_BY_LEVEL = linkedMapOf(
            Level.ALL to Log.VERBOSE,
            Level.TRACE to Log.VERBOSE,
            Level.DEBUG to Log.DEBUG,
            Level.INFO to Log.INFO,
            Level.WARN to Log.WARN,
            Level.ERROR to Log.ERROR,
        )

        /** A level unknown to logback (its constructor is private). */
        val CUSTOM_LEVEL: Level = Level::class.java
            .getDeclaredConstructor(Int::class.javaPrimitiveType, String::class.java)
            .apply { isAccessible = true }
            .newInstance(Level.INFO_INT + 1, "CUSTOM")
    }
}
