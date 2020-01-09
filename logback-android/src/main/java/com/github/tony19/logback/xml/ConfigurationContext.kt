package com.github.tony19.logback.xml

import ch.qos.logback.classic.LoggerContext
import java.util.*

data class ConfigurationContext(val loggerContext: LoggerContext = LoggerContext(),
                                val properties: Properties = Properties(),
                                var appenders: MutableList<ch.qos.logback.core.Appender<*>> = mutableListOf(),
                                val clock: IClock= SystemClock())
