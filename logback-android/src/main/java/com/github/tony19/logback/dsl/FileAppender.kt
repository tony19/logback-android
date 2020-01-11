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
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.util.OptionHelper

typealias MyFileAppender = FileAppender<ILoggingEvent>
fun Configuration.fileAppender(name: String = "file", block: FileAppender<ILoggingEvent>.() -> Unit = {}): FileAppender<ILoggingEvent> {
    val loggerContext = context
    return appender(::MyFileAppender) {
        this.name = name
        this.context = loggerContext
        encoder("%d - %msg%n")
        file("/tmp/logback%d.log")

        block()
        start()
    }
}

fun Logger.fileAppender(name: String = "file", block: FileAppender<ILoggingEvent>.() -> Unit = {}): FileAppender<ILoggingEvent> {
    val appender = FileAppender<ILoggingEvent>().apply {
        this.name = name
        context = loggerContext
        encoder("%d - %msg%n")
        file("/tmp/logback%d.log")

        block()
        start()
    }
    addAppender(appender)
    return appender
}

fun <E: ILoggingEvent> FileAppender<E>.encoder(pattern: String) {
    val context = this.context
    @Suppress("UNCHECKED_CAST")
    encoder = PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = context
        start()
    } as Encoder<E>
}

fun <E: ILoggingEvent> FileAppender<E>.file(name: String) {
    // TODO: Automatically convert relative path to absolute local-file path
    // Android requires absolute path
    file = OptionHelper.substVars(name, context)
}
