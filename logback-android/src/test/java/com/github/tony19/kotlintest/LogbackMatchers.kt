package com.github.tony19.kotlintest

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.status.StatusListener
import io.kotlintest.should

inline fun <reified T: Any> haveStatusListenerOfType() = haveElementOfType<T, StatusListener>()

inline fun <reified T: Any> haveAppenderOfType() = haveElementOfType<T, Appender<ILoggingEvent>>()

inline fun <reified T: Any> List<StatusListener>.shouldHaveStatusListenerOfType() = this should haveStatusListenerOfType<T>()

inline fun <reified T: Any> List<Appender<ILoggingEvent>>.shouldHaveAppenderOfType() = this should haveAppenderOfType<T>()