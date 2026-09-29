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
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.status.InfoStatus
import ch.qos.logback.core.status.Status
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the [BasicLogcatConfigurator] class
 */
@RunWith(RobolectricTestRunner::class)
class BasicLogcatConfiguratorTest {

    private val context = LoggerContext()
    private val cleanups = mutableListOf<() -> Unit>()

    @Before
    fun before() {
        ShadowLog.reset()
    }

    @After
    fun after() {
        cleanups.asReversed().forEach { it() }
        context.stop()
        ShadowLog.reset()
    }

    @Test
    fun `configure attaches a started logcat appender to the root logger`() {
        BasicLogcatConfigurator.configure(context)

        val appender = assertIs<LogcatAppender>(rootOf(context).getAppender("logcat"))
        assertTrue(appender.isStarted)
        assertSame(context, appender.context)
        assertFalse(appender.checkLoggable)
        assertNull(appender.tagEncoder)
        val encoder = assertIs<PatternLayoutEncoder>(appender.encoder)
        assertEquals("%msg", encoder.pattern)
        assertTrue(encoder.layout.isStarted)
        assertSame(context, encoder.context)
    }

    @Test
    fun `configure reports the default configuration and no errors`() {
        BasicLogcatConfigurator.configure(context)

        val statuses = context.statusManager.copyOfStatusList
        val setup = statuses.single { it.message == SETUP_MESSAGE }
        assertIs<InfoStatus>(setup)
        assertSame(context, setup.origin)
        assertEquals(emptyList(), statuses.filter { it.level == Status.ERROR })
    }

    @Test
    fun `configured context logs messages to logcat without trailing newline`() {
        BasicLogcatConfigurator.configure(context)

        context.getLogger("foo.Bar").debug("hello")
        context.getLogger("foo.Bar").error("oops")

        val logs = ShadowLog.getLogsForTag("foo.Bar").map { it.type to it.msg }
        assertEquals(listOf(Log.DEBUG to "hello", Log.ERROR to "oops"), logs)
    }

    @Test
    fun `configure works without a status manager`() {
        val realStatusManager = context.statusManager
        val lc = spy(context)
        doReturn(null).whenever(lc).statusManager

        BasicLogcatConfigurator.configure(lc)

        val appender = assertIs<LogcatAppender>(rootOf(lc).getAppender("logcat"))
        assertTrue(appender.isStarted)
        assertTrue(realStatusManager.copyOfStatusList.none { it.message == SETUP_MESSAGE })
        lc.getLogger("foo.Bar").info("hello")
        assertEquals(listOf("hello"), ShadowLog.getLogsForTag("foo.Bar").map { it.msg })
    }

    @Test
    fun `configureDefaultContext configures the SLF4J logger context`() {
        val defaultContext = assertIs<LoggerContext>(LoggerFactory.getILoggerFactory())
        val root = rootOf(defaultContext)
        val appendersBefore = appendersOf(root)
        val statusesBefore = defaultContext.statusManager.copyOfStatusList

        BasicLogcatConfigurator.configureDefaultContext()

        val added = appendersOf(root) - appendersBefore
        // leave the shared default context as we found it
        cleanups += { added.forEach { root.detachAppender(it); it.stop() } }

        val appender = assertIs<LogcatAppender>(added.single())
        assertEquals("logcat", appender.name)
        assertTrue(appender.isStarted)
        assertSame(defaultContext, appender.context)
        assertEquals("%msg", assertIs<PatternLayoutEncoder>(appender.encoder).pattern)
        // compare by identity: the status buffer is shared with the other tests and bounded
        val newStatuses = defaultContext.statusManager.copyOfStatusList.filter { s -> statusesBefore.none { it === s } }
        assertSame(defaultContext, newStatuses.single { it.message == SETUP_MESSAGE }.origin)
    }

    private fun rootOf(lc: LoggerContext): Logger = lc.getLogger(Logger.ROOT_LOGGER_NAME)

    private fun appendersOf(logger: Logger): Set<Appender<ILoggingEvent>> =
        logger.iteratorForAppenders().asSequence().toSet()

    private companion object {
        const val SETUP_MESSAGE = "Setting up default configuration."
    }
}
