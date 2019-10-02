package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.status.StatusListener
import com.github.tony19.kotlintest.shouldHaveElementOfType
import io.kotlintest.specs.StringSpec

class ConfigurationTest: StringSpec({
    "debug flag enables console listener" {

        val x = Configuration {
            debug(true)

            root {
                //        appenderRef("logcat", this@Configuration)
                logcatAppender()
            }
        }

        x.context.statusManager.copyOfStatusListenerList.shouldHaveElementOfType<OnConsoleStatusListener, StatusListener>()
    }

    "queues LogcatAppender" {
        val x = Configuration {
            logcatAppender()
        }

        x.appenders.shouldHaveElementOfType<LogcatAppender, Appender<ILoggingEvent>>()
    }

    "adds LogcatAppender to root logger" {
        val x = Configuration {
            root {
                logcatAppender()
            }
        }

        x.context.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders().asSequence().toList().shouldHaveElementOfType<LogcatAppender, Appender<ILoggingEvent>>()
    }
})
