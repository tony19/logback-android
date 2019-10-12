package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger

class Logback(val config: Configuration) {
    init {

    }

    fun getLogger(name: String?): Logger {
        return config.context.getLogger(name ?: Logger.ROOT_LOGGER_NAME)
    }
}