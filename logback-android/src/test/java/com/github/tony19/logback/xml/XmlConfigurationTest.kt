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
        "can be set to true" - {
            val config = XmlParser.parse("""<configuration debug="true"> </configuration>""")
            config.debug shouldBe true
        }

        "can be set to false" - {
            val config = XmlParser.parse("""<configuration debug="false"> </configuration>""")
            config.debug shouldBe false
        }

        "is falsy by default" - {
            val config = XmlParser.parse("""<configuration> </configuration>""")
            config.debug shouldNotBe true
        }
    }

    "scan attr" - {
        "can be set to true" - {
            val config = XmlParser.parse("""<configuration scan="true" />""")
            config.scan shouldBe true
        }

        "can be set to false" - {
            val config = XmlParser.parse("""<configuration scan="false" />""")
            config.scan shouldBe false
        }

        "is falsy by default" - {
            val config = XmlParser.parse("""<configuration />""")
            config.scan shouldNotBe true
        }
    }

    "scanPeriod attr" - {
        "can be set" - {
            val config = XmlParser.parse("""<configuration scan="true" scanPeriod="1 day" />""")
            config.scanPeriod shouldBe "1 day"
        }

        "is null by default" - {
            val config = XmlParser.parse("""<configuration />""")
            config.scanPeriod should beNull()
        }
    }

    "property" - {
        "sets value" - {
            val config = XmlParser.parse("""<configuration>
                |<property key="foo" value="lorem ipsum" />
                |<property key="bar" value="dolor" />
                |</configuration>""".trimMargin())

            config.properties!! shouldHaveSize 2
            config.properties!!.find { x -> x.key == "foo" && x.value == "lorem ipsum"} shouldNot beNull()
            config.properties!!.find { x -> x.key == "bar" && x.value == "dolor"} shouldNot beNull()
        }
    }

    "timestamp" - {
        "sets value" - {
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
        "sets value" - {
            val config = XmlParser.parse("""<configuration>
                |<include file="/path/to/foo.xml" />
                |</configuration>""".trimMargin())

            config.includes!! shouldHaveSize 1
            config.includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" - {
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
        "sets value" - {
            val config = XmlParser.parse("""<configuration>
                |<includes>
                |  <include file="/path/to/foo.xml" />
                |</includes>
                |</configuration>""".trimMargin())

            config.optionalIncludes!! shouldHaveSize 1
            config.optionalIncludes!![0].includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" - {
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
        "adds LogcatAppender meta" {
            val config = XmlParser.parse("""<configuration>
                |<appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
                |  <tagEncoder>
                |    <pattern>%logger{12}</pattern>
                |  </tagEncoder>
                |  <encoder>
                |    <pattern>[%-20thread] %msg</pattern>
                |  </encoder>
                |</appender>
                |</configuration>""".trimMargin())

            config.appenderMeta!! shouldHaveSize 1
            config.appenderMeta!!.find { it.name == "logcat" && it.className == "ch.qos.logback.classic.android.LogcatAppender" } shouldNot beNull()
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
                val logcat = appender as ch.qos.logback.classic.android.LogcatAppender
                logcat.tagEncoder shouldNot beNull()
                logcat.encoder shouldNot beNull()
                logcat.tagEncoder!!.pattern shouldBe "%logger{12}"
                logcat.encoder!!.pattern shouldBe "[%-20thread] %msg"
            }
        }
    }
})