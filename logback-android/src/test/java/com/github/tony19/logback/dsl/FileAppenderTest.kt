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
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.matchers.types.beNull
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.FreeSpec

class FileAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "fileAppender" - {
            "queues FileAppender" {
                val x = Configuration {
                    fileAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<FileAppender<ILoggingEvent>>()
            }

            "has default name" {
                val x = Configuration {
                    fileAppender()
                }
                x.appenders[0].name shouldBe "file"
            }

            "has default message encoder" {
                val x = Configuration {
                    fileAppender()
                }
                val appender = x.appenders[0] as FileAppender<ILoggingEvent>
                appender.encoder shouldNot beNull()
                appender.encoder.shouldBeInstanceOf<PatternLayoutEncoder>()
                (appender.encoder as PatternLayoutEncoder).pattern shouldNot beEmpty()
            }

            "receives filename" {
                val tmpFile = createTempFile("logback-android-test", "log")
                tmpFile.deleteOnExit()
                val x = Configuration {
                    fileAppender {
                        file(tmpFile.absolutePath)
                    }
                }
                val appender = x.appenders[0] as FileAppender<ILoggingEvent>
                appender.file shouldBe tmpFile.absolutePath
            }
        }

        "root" - {
            "receives fileAppender" {
                val x = Configuration {
                    root {
                        fileAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<FileAppender<ILoggingEvent>>()
            }
        }
    }
}
