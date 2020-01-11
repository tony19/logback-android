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
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

fun <T: Appender<ILoggingEvent>> Configuration.appender(appender: () -> T, block: T.() -> Unit = {}): T {
    val apdr = appender().apply(block)
    appenders.add(apdr)
    return apdr
}

fun <T> Logger.appenderRef(name: String, config: Configuration): T {
    @Suppress("UNCHECKED_CAST")
    return config.appenders.find { it.name == name }?.also {
        it.start()
        addAppender(it)
    } as T
}
