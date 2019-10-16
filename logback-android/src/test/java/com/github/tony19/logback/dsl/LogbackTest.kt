package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import io.kotlintest.specs.FreeSpec

class LogbackTest: FreeSpec({
    fun logback(block: Logger.() -> Unit): Logback {
        return Logback(Configuration {
            debug(true)
            root {
                block()
            }
        })
    }

    "logs events to logcat" {
        logback { logcatAppender() }.logger.info("hello world")
    }

    "logs events to file" {
        logback { fileAppender() }.logger.info("hello world")
    }

    "logs events to rolling files" {
        logback { rollingFileAppender() }.logger.info("hello world")
    }
})