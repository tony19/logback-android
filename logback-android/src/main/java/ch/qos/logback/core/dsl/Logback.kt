package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger

class Logback(val config: Configuration) {
    companion object {
        fun getLogger(name: String): Logger {
            return Logback(DefaultConfig).logger(name)
        }
    }

    val logger get() = logger()

    fun logger(name: String? = null): Logger {
        return config.context.getLogger(name ?: Logger.ROOT_LOGGER_NAME)
    }
}