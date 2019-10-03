package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import com.github.tony19.kotlintest.haveAppenderOfType
import com.github.tony19.kotlintest.haveStatusListenerOfType
import io.kotlintest.should
import io.kotlintest.shouldNot
import io.kotlintest.specs.FreeSpec

class ConfigurationTest: FreeSpec({
    "debug flag" - {
        "when true, enables console status listener" {
            val x = Configuration {
                debug(true)
            }
            x.context.statusManager.copyOfStatusListenerList should haveStatusListenerOfType<OnConsoleStatusListener>()
        }

        "when false, no console status listener" {
            val x = Configuration {
                debug(false)
            }
            x.context.statusManager.copyOfStatusListenerList shouldNot haveStatusListenerOfType<OnConsoleStatusListener>()
        }
    }

    "logcatAppender" - {
        "queues LogcatAppender" {
            val x = Configuration {
                logcatAppender()
            }
            x.appenders should haveAppenderOfType<LogcatAppender>()
        }

        "adds LogcatAppender to root logger" {
            val x = Configuration {
                root {
                    logcatAppender()
                }
            }
            x.context.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders().asSequence().toList() should haveAppenderOfType<LogcatAppender>()
        }
    }
})
