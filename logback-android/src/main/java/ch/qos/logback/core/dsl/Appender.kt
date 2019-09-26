package ch.qos.logback.core.dsl

import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

fun <T> Configuration.appender(appender: () -> T, block: T.() -> Unit = {})
    where T: Appender<ILoggingEvent> {
    appenders.add(appender().apply(block))
}

fun LogcatAppender.encoder(pattern: String) {
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        start()
    }
}

fun LogcatAppender.tagEncoder(pattern: String) {
    tagEncoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        start()
    }
}

fun Configuration.logcatAppender(name: String = "logcat", block: LogcatAppender.() -> Unit = {}) = appender(::LogcatAppender) {
    this.name = name
    encoder("%d - %msg%n")
    tagEncoder("%logger [%thread]")
    block()
}