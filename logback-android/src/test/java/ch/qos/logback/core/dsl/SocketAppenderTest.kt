package ch.qos.logback.core.dsl

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
