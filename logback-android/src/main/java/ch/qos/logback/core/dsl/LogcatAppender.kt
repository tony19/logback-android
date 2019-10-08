package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder


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

fun Logger.logcatAppender(name: String = "logcat", block: () -> Unit = {}) {
    val appender = LogcatAppender().apply {
        this.name = name
        context = loggerContext
        encoder("%d - %msg%n")
        tagEncoder("%logger [%thread]")
        start()

        block()
    }
    addAppender(appender)
}