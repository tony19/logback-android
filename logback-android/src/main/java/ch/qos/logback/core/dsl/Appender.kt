package ch.qos.logback.core.dsl

import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.rolling.*
import ch.qos.logback.core.util.FileSize

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

typealias MyFileAppender = FileAppender<ILoggingEvent>
fun Configuration.fileAppender(name: String = "file", block: FileAppender<ILoggingEvent>.() -> Unit = {}) {
    val loggerContext = context
    appender(::MyFileAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")
        file("/tmp/logback%d.log")

        start()

        block()
    }
}

fun <E: ILoggingEvent> FileAppender<E>.encoder(pattern: String) {
    val context = this.context
    @Suppress("UNCHECKED_CAST")
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    } as Encoder<E>
}

fun <E: ILoggingEvent> FileAppender<E>.file(name: String) {
    // TODO: Automatically convert relative path to absolute local-file path
    // Android requires absolute path
    file = name
}
