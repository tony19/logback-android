package com.github.tony19.logback.xml

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.beNull
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FreeSpec

class XmlConfigurationTest: FreeSpec({

    "debug attr" - {
        "can be set to true" {
            val config = XmlParser.parse("""<configuration debug="true"> </configuration>""")
            config.debug shouldBe true
        }

        "can be set to false" {
            val config = XmlParser.parse("""<configuration debug="false"> </configuration>""")
            config.debug shouldBe false
        }

        "is falsy by default" {
            val config = XmlParser.parse("""<configuration> </configuration>""")
            config.debug shouldNotBe true
        }
    }

    "scan attr" - {
        "can be set to true" {
            val config = XmlParser.parse("""<configuration scan="true" />""")
            config.scan shouldBe true
        }

        "can be set to false" {
            val config = XmlParser.parse("""<configuration scan="false" />""")
            config.scan shouldBe false
        }

        "is falsy by default" {
            val config = XmlParser.parse("""<configuration />""")
            config.scan shouldNotBe true
        }
    }

    "scanPeriod attr" - {
        "can be set" {
            val config = XmlParser.parse("""<configuration scan="true" scanPeriod="1 day" />""")
            config.scanPeriod shouldBe "1 day"
        }

        "is null by default" {
            val config = XmlParser.parse("""<configuration />""")
            config.scanPeriod should beNull()
        }
    }

    "property" - {
        "sets system property" {
            val config = XmlParser.parse("""<configuration>
            |<property key="logback.test.foo" value="bar" scope="system" />
            |</configuration>""".trimMargin())
            System.getProperty("logback.test.foo") shouldBe "bar"
            config.context.getProperty("logback.test.foo") shouldBe null
            config.properties["logback.test.foo"] shouldBe null
        }

        "sets context property" {
            val config = XmlParser.parse("""<configuration>
            |<property key="myKey" value="myValue" scope="context" />
            |</configuration>""".trimMargin())
            config.context.getProperty("myKey") shouldBe "myValue"
            config.properties["myKey"] shouldBe null
        }

        "sets local property" {
            val config = XmlParser.parse("""<configuration>
            |<property key="local1" value="value1" scope="local" />
            |<property key="local2" value="value2" />
            |</configuration>""".trimMargin())
            config.properties["local1"] shouldBe "value1"
            config.properties["local2"] shouldBe "value2"
            config.context.getProperty("local1") shouldBe null
            config.context.getProperty("local2") shouldBe null
        }

        "sets local property with variables" {
            val config = XmlParser.parse("""<configuration>
            |<property key="logdir" value="/path/to/logs" scope="local" />
            |<property key="logfile" value="${'$'}{logdir}/log.txt" />
            |</configuration>""".trimMargin())

            config.properties["logfile"] shouldBe "/path/to/logs/log.txt"
        }
    }

    "timestamp" - {
        "sets value" {
            val config = XmlParser.parse("""<configuration>
                |<timestamp key="foo" datePattern="yyyyMMdd'T'HHmmss" />
                |<timestamp key="bar" datePattern="yyyyMMdd" />
                |</configuration>""".trimMargin())

            config.timestamps!! shouldHaveSize 2
            config.timestamps!!.find { it.key == "foo" && it.datePattern == "yyyyMMdd'T'HHmmss" } shouldNot beNull()
            config.timestamps!!.find { it.key == "bar" && it.datePattern == "yyyyMMdd" } shouldNot beNull()
        }
    }

    "include" - {
        "sets value" {
            val config = XmlParser.parse("""<configuration>
                |<include file="/path/to/foo.xml" />
                |</configuration>""".trimMargin())

            config.includes!! shouldHaveSize 1
            config.includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" {
            val config = XmlParser.parse("""<configuration>
                |<include file="/path/to/foo.xml" />
                |<include file="/path/to/bar.xml" />
                |</configuration>""".trimMargin())

            config.includes!! shouldHaveSize 2
            config.includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
            config.includes!!.find { it.file == "/path/to/bar.xml" } shouldNot beNull()
        }
    }

    "includes" - {
        "sets value" {
            val config = XmlParser.parse("""<configuration>
                |<includes>
                |  <include file="/path/to/foo.xml" />
                |</includes>
                |</configuration>""".trimMargin())

            config.optionalIncludes!! shouldHaveSize 1
            config.optionalIncludes!![0].includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" {
            val config = XmlParser.parse("""<configuration>
                |<includes>
                |  <include file="/path/to/foo.xml" />
                |  <include file="/path/to/bar.xml" />
                |</includes>
                |<includes>
                |  <include file="/path/to/baz.xml" />
                |  <include file="/path/to/qux.xml" />
                |</includes>
                |</configuration>""".trimMargin())

            config.optionalIncludes!! shouldHaveSize 2
            config.optionalIncludes!![0].includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
            config.optionalIncludes!![0].includes!!.find { it.file == "/path/to/bar.xml" } shouldNot beNull()
            config.optionalIncludes!![1].includes!!.find { it.file == "/path/to/baz.xml" } shouldNot beNull()
            config.optionalIncludes!![1].includes!!.find { it.file == "/path/to/qux.xml" } shouldNot beNull()
        }
    }

    "appender" - {
        "ignores unreferenced appenders" {
            val config = XmlParser.parse("""<configuration>
                |<appender name="unusedA" class="ch.qos.logback.classic.android.LogcatAppender">
                |  <tagEncoder>
                |    <pattern>%logger{12}</pattern>
                |  </tagEncoder>
                |  <encoder>
                |    <pattern>[%-20thread] %msg</pattern>
                |  </encoder>
                |</appender>
                |<appender name="unusedB" class="ch.qos.logback.classic.android.LogcatAppender">
                |  <tagEncoder>
                |    <pattern>%logger{12}</pattern>
                |  </tagEncoder>
                |  <encoder>
                |    <pattern>[%-20thread] %msg</pattern>
                |  </encoder>
                |</appender>
                |</configuration>""".trimMargin())

            config.appenders shouldHaveSize 0
        }

        "creates LogcatAppender instance" - {
            val config = XmlParser.parse("""<configuration>
                |<appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
                |  <tagEncoder>
                |    <pattern>%logger{12}</pattern>
                |  </tagEncoder>
                |  <encoder>
                |    <pattern>[%-20thread] %msg</pattern>
                |  </encoder>
                |</appender>
                |<root>
                |  <appender-ref ref="logcat" />
                |</root>
                |</configuration>""".trimMargin())

            val appender = config.appenders.find { it.name == "logcat" }

            appender shouldNot beNull()
            appender.shouldBeInstanceOf<ch.qos.logback.classic.android.LogcatAppender>()

            "and configures it" {
                (appender as ch.qos.logback.classic.android.LogcatAppender).apply {
                    tagEncoder shouldNot beNull()
                    encoder shouldNot beNull()
                    tagEncoder!!.pattern shouldBe "%logger{12}"
                    encoder!!.pattern shouldBe "[%-20thread] %msg"
                }
            }
        }

        "creates FileAppender instance" - {
            val config = XmlParser.parse("""<configuration>
                <appender name="file" class="ch.qos.logback.core.FileAppender">
                  <lazy>true</lazy>
                  <file>/data/data/com.example/files/log.txt</file>
                  <encoder>
                    <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %msg%n</pattern>
                  </encoder>
                </appender>
                <root>
                  <appender-ref ref="file"/>
                </root>
                </configuration>""")

            val appender = config.appenders.find { it.name == "file" }

            appender shouldNot beNull()
            appender.shouldBeInstanceOf<ch.qos.logback.core.FileAppender<*>>()

            "and configures it" {
                (appender as ch.qos.logback.core.FileAppender<*>).apply {
                    encoder shouldNot beNull()
                    (encoder!! as ch.qos.logback.classic.encoder.PatternLayoutEncoder).pattern shouldBe "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %msg%n"
                    file shouldBe "/data/data/com.example/files/log.txt"
                    lazy shouldBe true
                }
            }
        }
    }
})