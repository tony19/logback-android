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

        start()

        block()
    }
}

typealias MyRollingFileAppender = RollingFileAppender<ILoggingEvent>
fun Configuration.rollingFileAppender(name: String = "rollingFile", block: RollingFileAppender<ILoggingEvent>.() -> Unit = {}) {
    val loggerContext = context
    appender(::MyRollingFileAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")

        val parent = this
        rollingPolicy(::FixedWindowRollingPolicy) {
            setParent(parent)

            // TODO: Make this an absolute path to the app's data dir
            fileNamePattern = "/sdcard/%d-%i.log.gz"
            file("/sdcard/%d.log")

            context = loggerContext
            start()
        }
        triggeringPolicy(::SizeBasedTriggeringPolicy) {
            val policy = this as SizeBasedTriggeringPolicy
            policy.maxFileSize = FileSize.valueOf("1MB")
            context = loggerContext
            start()
        }
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

fun <E: ILoggingEvent> RollingFileAppender<E>.encoder(pattern: String) {
    val context = this.context
    @Suppress("UNCHECKED_CAST")
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    } as Encoder<E>
}

fun <E: ILoggingEvent> RollingFileAppender<E>.file(name: String) {
    // TODO: Automatically convert relative path to absolute local-file path
    // Android requires absolute path
    file = name
}

fun <E: ILoggingEvent, R: RollingPolicy> RollingFileAppender<E>.rollingPolicy(policy: () -> R, block: R.() -> Unit = {}) {
    rollingPolicy = policy().apply(block)
}

typealias MySizeBasedTriggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>
fun <E: ILoggingEvent, R: TriggeringPolicy<E>> RollingFileAppender<E>.triggeringPolicy(policy: () -> R, block: R.() -> Unit = {}) {
    triggeringPolicy = policy().apply(block)
}

