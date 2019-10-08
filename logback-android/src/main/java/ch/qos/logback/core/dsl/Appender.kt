package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

fun <T> Configuration.appender(appender: () -> T, block: T.() -> Unit = {})
    where T: Appender<ILoggingEvent> {
    appenders.add(appender().apply(block))
}

fun Logger.appenderRef(name: String, config: Configuration) {
    config.appenders.find { it.name == name }?.let {
        it.start()
        addAppender(it)
    }
}
