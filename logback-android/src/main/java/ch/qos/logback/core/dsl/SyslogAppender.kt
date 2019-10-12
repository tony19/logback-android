package ch.qos.logback.core.dsl

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.net.SyslogAppender

fun Configuration.syslogAppender(name: String = "syslog", block: SyslogAppender.() -> Unit = {}) {
    val loggerContext = context
    appender(::SyslogAppender) {
        this.name = name
        context = loggerContext
        facility = "syslog"

        block()
        start()
    }
}

fun Logger.syslogAppender(name: String = "syslog", block: SyslogAppender.() -> Unit = {}) {
    val appender = SyslogAppender().apply {
        this.name = name
        context = loggerContext
        facility = "syslog"

        block()
        start()
    }
    addAppender(appender)
}
