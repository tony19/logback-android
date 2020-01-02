package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.util.FileSize
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.matchers.types.beNull
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.FreeSpec

class RollingFileAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "rollingFileAppender" - {
            "queues RollingFileAppender" {
                val x = Configuration {
                    rollingFileAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<RollingFileAppender<ILoggingEvent>>()
            }

            "has default name" {
                val x = Configuration {
                    rollingFileAppender()
                }
                x.appenders[0].name shouldBe "rollingFile"
            }

            "has default message encoder" {
                val x = Configuration {
                    rollingFileAppender()
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.encoder shouldNot beNull()
                appender.encoder.shouldBeInstanceOf<PatternLayoutEncoder>()
                (appender.encoder as PatternLayoutEncoder).pattern shouldNot beEmpty()
            }

            "has default rolling policy" {
                val x = Configuration {
                    rollingFileAppender()
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.rollingPolicy shouldNot beNull()
            }

            "has default triggering policy" {
                val x = Configuration {
                    rollingFileAppender()
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.triggeringPolicy shouldNot beNull()
            }

            "receives filename" {
                val tmpFile = createTempFile("logback-android-test", "log")
                tmpFile.deleteOnExit()
                val x = Configuration {
                    rollingFileAppender {
                        file(tmpFile.absolutePath)
                    }
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.file shouldBe tmpFile.absolutePath
            }

            "receives rollingPolicy" {
                val x = Configuration {
                    rollingFileAppender {
                        rollingPolicy(::FixedWindowRollingPolicy)
                    }
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.rollingPolicy shouldNot beNull()
                appender.rollingPolicy.shouldBeInstanceOf<FixedWindowRollingPolicy>()
            }

            "receives triggeringPolicy" {
                val x = Configuration {
                    rollingFileAppender {
                        triggeringPolicy(::MySizeBasedTriggeringPolicy) {
                            maxFileSize = FileSize.valueOf("1MB")
                        }
                    }
                }
                val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
                appender.triggeringPolicy shouldNot beNull()
                appender.triggeringPolicy.shouldBeInstanceOf<MySizeBasedTriggeringPolicy>()
            }
        }

        "root" - {
            "receives rollingFileAppender" {
                val x = Configuration {
                    root {
                        rollingFileAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<RollingFileAppender<ILoggingEvent>>()
            }
        }
    }
}
