package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME
import ch.qos.logback.classic.android.LogcatAppender

fun Configuration.logger(name: String, block: Logger.() -> Unit = {}) {
    context.getLogger(name).apply(block)
}

fun Configuration.root(block: Logger.() -> Unit = {}) = logger(ROOT_LOGGER_NAME, block)

fun Logger.appenderRef(name: String, config: Configuration) {
    config.appenders.find { it.name == name }?.let {
        it.start()
        addAppender(it)
    }
}

fun Logger.logcatAppender(name: String = "logcat", block: () -> Unit = {}) {
    val appender = LogcatAppender().apply {
        this.name = name
        context = loggerContext
        encoder("%d - %msg%n")
        tagEncoder("%logger [%thread]")
        start()

        block()
    }
    addAppender(appender)
}