package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.StatusListenerConfigHelper

open class Configuration(val context: LoggerContext = LoggerContext(), block : Configuration.() -> Unit = {}) {
    val appenders = mutableListOf<Appender<ILoggingEvent>>()

    init {
        block()
    }

    fun debug(enabled: Boolean) {
        if (enabled) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context, OnConsoleStatusListener())
        }
    }

    val appendersList
        get() = context.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders().asSequence().toList()
}
