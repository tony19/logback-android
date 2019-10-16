package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class LoggerTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "root" - {
            "has default name" {
                val x = Configuration {
                    root()
                }
                x.context.loggerList shouldHaveSize 1
                x.context.loggerList[0].name shouldBe Logger.ROOT_LOGGER_NAME
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
                    val apdr = appender(::LogcatAppender) {
                        name = "myAppender"
                    }
                    logger("myLogger") {
                        appenderRef(apdr.name, this@Configuration)
                    }
                }
                val appendersList = x.appendersList("myLogger")
                appendersList shouldHaveSize 1
                appendersList[0].name shouldBe "myAppender"
                appendersList[0].shouldBeInstanceOf<LogcatAppender>()
            }
        }
    }
}
