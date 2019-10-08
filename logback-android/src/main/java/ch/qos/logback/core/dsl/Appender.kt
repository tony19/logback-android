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
