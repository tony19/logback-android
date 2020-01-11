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
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

fun LogcatAppender.encoder(pattern: String) {
    val context = this.context
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    }
}

fun LogcatAppender.tagEncoder(pattern: String) {
    val context = this.context
    tagEncoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    }
}

fun Configuration.logcatAppender(name: String = "logcat", block: LogcatAppender.() -> Unit = {}): LogcatAppender {
    val loggerContext = context
    return appender(::LogcatAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")
        tagEncoder("%logger [%thread]")

        block()
        start()
    }
}

fun Logger.logcatAppender(name: String = "logcat", block: LogcatAppender.() -> Unit = {}): LogcatAppender {
    val appender = LogcatAppender().apply {
        this.name = name
        context = loggerContext
        encoder("%d - %msg%n")
        tagEncoder("%logger [%thread]")

        block()
        start()
    }
    addAppender(appender)
    return appender
}