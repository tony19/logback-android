package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.net.SocketAppender

fun Configuration.socketAppender(name: String = "socket", block: SocketAppender.() -> Unit = {}) {
    val loggerContext = context
    appender(::SocketAppender) {
        this.name = name
        context = loggerContext

        block()
        start()
    }
}

fun Logger.socketAppender(name: String = "socket", block: SocketAppender.() -> Unit = {}) {
    val appender = SocketAppender().apply {
        this.name = name
        context = loggerContext

        block()
        start()
    }
    addAppender(appender)
}
