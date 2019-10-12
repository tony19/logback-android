package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.util.OptionHelper

typealias MyFileAppender = FileAppender<ILoggingEvent>
fun Configuration.fileAppender(name: String = "file", block: FileAppender<ILoggingEvent>.() -> Unit = {}) {
    val loggerContext = context
    appender(::MyFileAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")
        file("/tmp/logback%d.log")

        block()
        start()
    }
}

fun Logger.fileAppender(name: String = "file", block: () -> Unit = {}) {
    val appender = FileAppender<ILoggingEvent>().apply {
        this.name = name
        context = loggerContext
        encoder("%d - %msg%n")
        file("/tmp/logback%d.log")

        block()
        start()
    }
    addAppender(appender)
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
    file = OptionHelper.substVars(name, context)
}
