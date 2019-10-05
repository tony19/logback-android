package ch.qos.logback.core.dsl

import io.kotlintest.specs.FreeSpec

class ConfigurationIntegrationTest: FreeSpec({
    "logs events to logcat" {
        val x = Configuration {
            debug(true)
            root {
                logcatAppender()
            }
        }
        x.context.getLogger(ConfigurationIntegrationTest::class.simpleName).info("hello world")
    }

    "logs events to file" {
        val x = Configuration {
            debug(true)
            root {
                fileAppender()
            }
        }
        x.context.getLogger(ConfigurationIntegrationTest::class.simpleName).info("hello world")
    }

    "logs events to rolling files" {

    }
})