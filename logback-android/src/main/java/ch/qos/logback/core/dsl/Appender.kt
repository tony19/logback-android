package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

fun <T: Appender<ILoggingEvent>> Configuration.appender(appender: () -> T, block: T.() -> Unit = {}): T {
    val apdr = appender().apply(block)
    appenders.add(apdr)
    return apdr
}

fun <T> Logger.appenderRef(name: String, config: Configuration): T {
    @Suppress("UNCHECKED_CAST")
    return config.appenders.find { it.name == name }?.also {
        it.start()
        addAppender(it)
    } as T
}
