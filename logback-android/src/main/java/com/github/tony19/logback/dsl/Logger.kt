package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME

fun Configuration.logger(name: String, block: Logger.() -> Unit = {}): Logger {
    return context.getLogger(name).apply(block)
}

fun Configuration.root(block: Logger.() -> Unit = {}) = logger(ROOT_LOGGER_NAME, block)
