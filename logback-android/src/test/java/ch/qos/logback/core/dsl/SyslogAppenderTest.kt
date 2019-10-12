package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.net.SyslogAppender
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class SyslogAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "syslogFileAppender" - {
            "queues SyslogAppender" {
                val x = Configuration {
                    syslogAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<SyslogAppender>()
            }

            "has default name" {
                val x = Configuration {
                    syslogAppender()
                }
                x.appenders[0].name shouldBe "syslog"
            }
        }

        "root" - {
            "receives syslogAppender" {
                val x = Configuration {
                    root {
                        syslogAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<SyslogAppender>()
            }
        }
    }
}
