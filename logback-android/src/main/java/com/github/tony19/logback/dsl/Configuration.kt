/*
 * Copyright (c) 2020 Anthony Trinh.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
