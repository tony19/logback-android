package com.github.tony19.logback.dsl

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.spi.ContextAware
import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.OptionHelper
import ch.qos.logback.core.util.StatusListenerConfigHelper
import java.util.*
import kotlin.collections.HashMap

open class Configuration(val context: LoggerContext = LoggerContext(), block : Configuration.() -> Unit = {}) {
    val appenders = mutableListOf<Appender<ILoggingEvent>>()
    val props = HashMap<String, String>()

    init {
        block()
    }

    fun debug(enabled: Boolean) {
        if (enabled) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context, OnConsoleStatusListener())
        }
    }

    fun property(key: String, value: String, scope: String = "local") {
        val resolvedValue = OptionHelper.substVars(value, context)
        val listener =
            if (context.statusManager.copyOfStatusListenerList.size > 0)
                context.statusManager.copyOfStatusListenerList[0] as ContextAware
            else ContextAwareBase()

        when (scope.toLowerCase(Locale.US)) {
            "context" -> context.putProperty(key, resolvedValue)
            "system" -> OptionHelper.setSystemProperty(listener, key, resolvedValue)
            else -> props.put(key, resolvedValue)
        }
    }
}
