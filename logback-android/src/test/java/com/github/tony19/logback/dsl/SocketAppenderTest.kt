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
import ch.qos.logback.classic.net.SocketAppender
import ch.qos.logback.core.util.Duration
import com.github.tony19.kotlintest.haveAppenderOfType
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec

class SocketAppenderTest: FreeSpec() {
    fun Configuration.appendersList(name: String = Logger.ROOT_LOGGER_NAME) = context.getLogger(name).iteratorForAppenders().asSequence().toList()

    init {
        "socketAppender" - {
            "queues SocketAppender" {
                val x = Configuration {
                    socketAppender()
                }
                x.appenders shouldHaveSize 1
                x.appenders should haveAppenderOfType<SocketAppender>()
            }

            "has default name" {
                val x = Configuration {
                    socketAppender()
                }
                x.appenders[0].name shouldBe "socket"
            }

            "receives port number" {
                val x = Configuration {
                    socketAppender {
                        port = 3456
                    }
                }
                val socket = x.appenders[0] as SocketAppender
                socket.port shouldBe 3456
            }

            "receives remote host" {
                val x = Configuration {
                    socketAppender {
                        remoteHost = "foo.socket.net"
                    }
                }
                val socket = x.appenders[0] as SocketAppender
                socket.remoteHost shouldBe "foo.socket.net"
            }

            "receives event delay limit" {
                val x = Configuration {
                    socketAppender {
                        eventDelayLimit = Duration(1000)
                    }
                }
                val socket = x.appenders[0] as SocketAppender
                socket.eventDelayLimit.milliseconds shouldBe 1000
            }

            "receives queue size" {
                val x = Configuration {
                    socketAppender {
                        queueSize = 10
                    }
                }
                val socket = x.appenders[0] as SocketAppender
                socket.queueSize shouldBe 10
            }

            "receives reconnection delay" {
                val x = Configuration {
                    socketAppender {
                        reconnectionDelay = Duration(2000)
                    }
                }
                val socket = x.appenders[0] as SocketAppender
                socket.reconnectionDelay.milliseconds shouldBe 2000
            }
        }

        "root" - {
            "receives socketAppender" {
                val x = Configuration {
                    root {
                        socketAppender()
                    }
                }
                x.appendersList() should haveAppenderOfType<SocketAppender>()
            }
        }
    }
}
