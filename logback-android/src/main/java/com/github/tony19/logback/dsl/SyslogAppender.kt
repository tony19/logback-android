package com.github.tony19.logback.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.net.SyslogAppender

fun Configuration.syslogAppender(name: String = "syslog", block: SyslogAppender.() -> Unit = {}): SyslogAppender {
    val loggerContext = context
    return appender(::SyslogAppender) {
        this.name = name
        context = loggerContext
        facility = "syslog"

        block()
        start()
    }
}

fun Logger.syslogAppender(name: String = "syslog", block: SyslogAppender.() -> Unit = {}): SyslogAppender {
    val appender = SyslogAppender().apply {
        this.name = name
        context = loggerContext
        facility = "syslog"

        block()
        start()
    }
    addAppender(appender)
    return appender
}
