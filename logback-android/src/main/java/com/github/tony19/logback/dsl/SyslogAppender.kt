/*
 * Copyright (c) 2020 Anthony Trinh.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
