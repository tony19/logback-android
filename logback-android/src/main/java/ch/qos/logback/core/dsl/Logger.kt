package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME

fun Configuration.logger(name: String, block: Logger.() -> Unit = {}) {
    context.getLogger(name).apply(block)
}

fun Configuration.root(block: Logger.() -> Unit = {}) = logger(ROOT_LOGGER_NAME, block)
