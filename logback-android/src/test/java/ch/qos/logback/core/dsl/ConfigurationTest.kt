package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import com.github.tony19.kotlintest.haveAppenderOfType
import com.github.tony19.kotlintest.haveStatusListenerOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.matchers.types.beNull
import io.kotlintest.should
import io.kotlintest.shouldBe
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
            x.appenders shouldHaveSize 1
            x.appenders should haveAppenderOfType<LogcatAppender>()
        }

        "has default name `logcat`" {
            val x = Configuration {
                logcatAppender()
            }
            x.appenders[0].name shouldBe "logcat"
        }

        "has default message encoder" {
            val x = Configuration {
                logcatAppender()
            }
            val logcat = x.appenders[0] as LogcatAppender
            logcat.encoder shouldNot beNull()
            logcat.encoder.pattern shouldNot beEmpty()
        }

        "has default tag encoder" {
            val x = Configuration {
                logcatAppender()
            }
            val logcat = x.appenders[0] as LogcatAppender
            logcat.tagEncoder shouldNot beNull()
            logcat.tagEncoder.pattern shouldNot beEmpty()
        }

        "accepts name param" {
            val x = Configuration {
                logcatAppender("myLogger")
            }
            x.appenders[0].name shouldBe "myLogger"
        }

        "receives message encoder" {
            val pattern = "** %thread : %msg%n"
            val x = Configuration {
                logcatAppender {
                    encoder(pattern)
                }
            }
            val logcat = x.appenders[0] as LogcatAppender
            logcat.encoder shouldNot beNull()
            logcat.encoder.pattern shouldBe pattern
        }

        "receives tag encoder" {
            val pattern = "[%l]"
            val x = Configuration {
                logcatAppender {
                    tagEncoder(pattern)
                }
            }
            val logcat = x.appenders[0] as LogcatAppender
            logcat.tagEncoder shouldNot beNull()
            logcat.tagEncoder.pattern shouldBe pattern
        }
    }

    "root" - {
        "has default name `${Logger.ROOT_LOGGER_NAME}`" {
            val x = Configuration {
                root()
            }
            x.context.loggerList shouldHaveSize 1
            x.context.loggerList[0].name shouldBe Logger.ROOT_LOGGER_NAME
        }

        "receives logcatAppender" {
            val x = Configuration {
                root {
                    logcatAppender()
                }
            }
            x.appendersList should haveAppenderOfType<LogcatAppender>()
        }
    }

    "logger" - {
        "accepts name param" {
            val x = Configuration {
                logger("myLogger")
            }
            x.context.loggerList shouldHaveSize 2 // root + custom logger
            x.context.loggerList[1].name shouldBe "myLogger"
        }

        "receives appender refs" {
            val x = Configuration {
                appender(::LogcatAppender) {
                   name = "myAppender"
                }
                logger("myLogger") {
                    appenderRef("myAppender", this@Configuration)
                }
            }
            val appendersList = x.context.getLogger("myLogger").iteratorForAppenders().asSequence().toList()
            appendersList shouldHaveSize 1
            appendersList[0].name shouldBe "myAppender"
        }
    }
})
