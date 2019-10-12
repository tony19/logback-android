package ch.qos.logback.core.dsl

import io.kotlintest.specs.FreeSpec

class LogbackTest: FreeSpec({
    "logs events to logcat" {
        val x = Configuration {
            debug(true)
            root {
                logcatAppender()
            }
        }
        Logback(x).getLogger(LogbackTest::class.simpleName).info("hello world")
    }

    "logs events to file" {
        val x = Configuration {
            debug(true)
            root {
                fileAppender()
            }
        }
        Logback(x).getLogger(LogbackTest::class.simpleName).info("hello world")
    }

    "logs events to rolling files" {

    }
})