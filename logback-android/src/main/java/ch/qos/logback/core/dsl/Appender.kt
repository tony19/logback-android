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
    val context = this.context
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    }
}

fun LogcatAppender.tagEncoder(pattern: String) {
    val context = this.context
    tagEncoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    }
}

fun Configuration.logcatAppender(name: String = "logcat", block: LogcatAppender.() -> Unit = {}) {
    val loggerContext = context
    appender(::LogcatAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")
        tagEncoder("%logger [%thread]")
        start()

        block()
    }
}