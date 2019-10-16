package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.net.SocketAppender

fun Configuration.socketAppender(name: String = "socket", block: SocketAppender.() -> Unit = {}): SocketAppender {
    val loggerContext = context
    return appender(::SocketAppender) {
        this.name = name
        context = loggerContext

        block()
        start()
    }
}

fun Logger.socketAppender(name: String = "socket", block: SocketAppender.() -> Unit = {}): SocketAppender {
    val appender = SocketAppender().apply {
        this.name = name
        context = loggerContext

        block()
        start()
    }
    addAppender(appender)
    return appender
}
