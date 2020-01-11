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
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class LoggerTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "root" - {
            "has default name" {
                val x = Configuration {
                    root()
                }
                x.context.loggerList shouldHaveSize 1
                x.context.loggerList[0].name shouldBe Logger.ROOT_LOGGER_NAME
            }
        }

        "logger" - {
            "accepts name param" {
                val x = Configuration {
                    logger("myLogger")
                }
                x.context.loggerList shouldHaveSize 2 // root + custom logger
                x.context.loggerList[1].name shouldBe "myLogger"
            }

            "receives appender refs" {
                val x = Configuration {
                    val apdr = appender(::LogcatAppender) {
                        name = "myAppender"
                    }
                    logger("myLogger") {
                        appenderRef(apdr.name, this@Configuration)
                    }
                }
                val appendersList = x.appendersList("myLogger")
                appendersList shouldHaveSize 1
                appendersList[0].name shouldBe "myAppender"
                appendersList[0].shouldBeInstanceOf<LogcatAppender>()
            }
        }
    }
}
