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
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class SyslogAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "syslogFileAppender" - {
            "queues SyslogAppender" {
                val x = Configuration {
                    syslogAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<SyslogAppender>()
            }

            "has default name" {
                val x = Configuration {
                    syslogAppender()
                }
                x.appenders[0].name shouldBe "syslog"
            }

            "receives port number" {
                val x = Configuration {
                    syslogAppender {
                        port = 3456
                    }
                }
                val syslog = x.appenders[0] as SyslogAppender
                syslog.port shouldBe 3456
            }

            "receives remote host" {
                val x = Configuration {
                    syslogAppender {
                        syslogHost = "foo.syslog.net"
                    }
                }
                val syslog = x.appenders[0] as SyslogAppender
                syslog.syslogHost shouldBe "foo.syslog.net"
            }

            "receives max message size" {
                val x = Configuration {
                    syslogAppender {
                        maxMessageSize = 123456
                    }
                }
                val syslog = x.appenders[0] as SyslogAppender
                syslog.maxMessageSize shouldBe 123456
            }

            "receives suffix pattern" {
                val x = Configuration {
                    syslogAppender {
                        suffixPattern = "\n\n"
                    }
                }
                val syslog = x.appenders[0] as SyslogAppender
                syslog.suffixPattern shouldBe "\n\n"
            }

            "receives stack trace pattern" {
                val x = Configuration {
                    syslogAppender {
                        stackTracePattern = "\t\t\t"
                    }
                }
                val syslog = x.appenders[0] as SyslogAppender
                syslog.stackTracePattern shouldBe "\t\t\t"
            }
        }

        "root" - {
            "receives syslogAppender" {
                val x = Configuration {
                    root {
                        syslogAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<SyslogAppender>()
            }
        }
    }
}
