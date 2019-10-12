package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.matchers.types.beNull
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.FreeSpec

class LogcatAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "logcatAppender" - {
            "queues LogcatAppender" {
                val x = Configuration {
                    logcatAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<LogcatAppender>()
            }

            "has default name" {
                val x = Configuration {
                    logcatAppender()
                }
                x.appenders[0].name shouldBe "logcat"
            }

            "has default message encoder" {
                val x = Configuration {
                    logcatAppender()
                }
                val appender = x.appenders[0] as LogcatAppender
                appender.encoder shouldNot beNull()
                appender.encoder.pattern shouldNot beEmpty()
            }

            "has default tag encoder" {
                val x = Configuration {
                    logcatAppender()
                }
                val appender = x.appenders[0] as LogcatAppender
                appender.tagEncoder shouldNot beNull()
                appender.tagEncoder.pattern shouldNot beEmpty()
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
                val appender = x.appenders[0] as LogcatAppender
                appender.encoder shouldNot beNull()
                appender.encoder.pattern shouldBe pattern
            }

            "receives tag encoder" {
                val pattern = "[%l]"
                val x = Configuration {
                    logcatAppender {
                        tagEncoder(pattern)
                    }
                }
                val appender = x.appenders[0] as LogcatAppender
                appender.tagEncoder shouldNot beNull()
                appender.tagEncoder.pattern shouldBe pattern
            }
        }

        "root" - {
            "receives logcatAppender" {
                val x = Configuration {
                    root {
                        logcatAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<LogcatAppender>()
            }
        }
    }
}
