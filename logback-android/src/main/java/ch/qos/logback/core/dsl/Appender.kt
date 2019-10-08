package ch.qos.logback.core.dsl

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

fun <T> Configuration.appender(appender: () -> T, block: T.() -> Unit = {})
    where T: Appender<ILoggingEvent> {
    appenders.add(appender().apply(block))
}
