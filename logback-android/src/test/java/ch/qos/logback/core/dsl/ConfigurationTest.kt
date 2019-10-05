package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.FileSize
import com.github.tony19.kotlintest.haveAppenderOfType
import com.github.tony19.kotlintest.haveStatusListenerOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.matchers.types.beNull
import io.kotlintest.matchers.types.shouldBeInstanceOf
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

    "fileAppender" - {
        "queues FileAppender" {
            val x = Configuration {
                fileAppender()
            }
            x.appenders shouldHaveSize 1
            x.appenders should haveAppenderOfType<FileAppender<ILoggingEvent>>()
        }

        "has default name" {
            val x = Configuration {
                fileAppender()
            }
            x.appenders[0].name shouldBe "file"
        }

        "has default message encoder" {
            val x = Configuration {
                fileAppender()
            }
            val appender = x.appenders[0] as FileAppender<ILoggingEvent>
            appender.encoder shouldNot beNull()
            appender.encoder.shouldBeInstanceOf<PatternLayoutEncoder>()
            (appender.encoder as PatternLayoutEncoder).pattern shouldNot beEmpty()
        }

        "receives filename" {
            val filename = "foo/bar.log"
            val x = Configuration {
                fileAppender {
                    file(filename)
                }
            }
            val appender = x.appenders[0] as FileAppender<ILoggingEvent>
            appender.file shouldBe filename
        }
    }

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
            val filename = "foo/bar.log"
            val x = Configuration {
                rollingFileAppender {
                    file(filename)
                }
            }
            val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
            appender.file shouldBe filename
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
                        val policy = this as MySizeBasedTriggeringPolicy
                        policy.maxFileSize = FileSize.valueOf("1MB")
                    }
                }
            }
            val appender = x.appenders[0] as RollingFileAppender<ILoggingEvent>
            appender.triggeringPolicy shouldNot beNull()
            appender.triggeringPolicy.shouldBeInstanceOf<MySizeBasedTriggeringPolicy>()
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
        "has default name" {
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
            x.appendersList() should haveAppenderOfType<LogcatAppender>()
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
            val appendersList = x.appendersList("myLogger")
            appendersList shouldHaveSize 1
            appendersList[0].name shouldBe "myAppender"
            appendersList[0].shouldBeInstanceOf<LogcatAppender>()
        }
    }
})
