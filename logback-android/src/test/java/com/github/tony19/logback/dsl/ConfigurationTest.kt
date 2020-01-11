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

import ch.qos.logback.core.status.OnConsoleStatusListener
import com.github.tony19.kotlintest.haveStatusListenerOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.FreeSpec

class ConfigurationTest: FreeSpec() {
    init {
        "debug flag" - {
            "when true, enables console status listener" {
                val x = Configuration {
                    debug(true)
                }
                x.context.statusManager.copyOfStatusListenerList should haveStatusListenerOfType<OnConsoleStatusListener>()
            }

            "when false, no console status listener" {
                val x = Configuration {
                    debug(false)
                }
                x.context.statusManager.copyOfStatusListenerList shouldNot haveStatusListenerOfType<OnConsoleStatusListener>()
            }
        }

        "property" - {
            "sets local prop by default" {
                val key = "logDir"
                val dirPath = "/path/to/log"
                val x = Configuration {
                    property(key, dirPath)
                }
                x.props.entries shouldHaveSize 1
                x.props[key] shouldBe dirPath
            }

            "sets local prop" {
                val key = "logDir"
                val dirPath = "/path/to/log"
                val x = Configuration {
                    property(key, dirPath, "local")
                }
                x.props.entries shouldHaveSize 1
                x.props[key] shouldBe dirPath
            }

            "sets system prop" {
                val key = "logDir"
                val dirPath = "/path/to/log"
                val x = Configuration {
                    property(key, dirPath, "system")
                }
                x.props shouldBe emptyMap<String, String>()
                System.getProperty(key) shouldBe dirPath
            }

            "sets context prop" {
                val key = "logDir"
                val dirPath = "/path/to/log"
                val x = Configuration {
                    property(key, dirPath, "context")
                }
                x.props shouldBe emptyMap<String, String>()
                x.context.getProperty(key) shouldBe dirPath
            }
        }
    }
}
