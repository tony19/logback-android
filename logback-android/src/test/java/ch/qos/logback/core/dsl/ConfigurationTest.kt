package ch.qos.logback.core.dsl

import io.kotlintest.specs.StringSpec

class ConfigurationTest: StringSpec({
    "config" {

        val x = Configuration {
            debug(true)

//    appender(::LogcatAppender) {
//        name = "logcat"
//        encoder("%d - %msg%n")
//        tagEncoder("%logger [%thread]")
//    }
//
//    logcatAppender()

            root {
                //        appenderRef("logcat", this@Configuration)
                logcatAppender()
            }
        }

    }
})