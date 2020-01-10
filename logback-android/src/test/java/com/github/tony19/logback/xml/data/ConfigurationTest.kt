package com.github.tony19.logback.xml.data

import com.github.tony19.logback.xml.XmlParser
import io.kotlintest.*
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.beNull
import io.kotlintest.specs.FreeSpec

class ConfigurationTest: FreeSpec({

    "debug attr" - {
        "can be set to true" {
            val config = XmlParser.parseConfigurationData("""<configuration debug="true"> </configuration>""")
            config.debug shouldBe true
        }

        "can be set to false" {
            val config = XmlParser.parseConfigurationData("""<configuration debug="false"> </configuration>""")
            config.debug shouldBe false
        }

        "is falsy by default" {
            val config = XmlParser.parseConfigurationData("""<configuration> </configuration>""")
            config.debug shouldNotBe true
        }
    }

    "scan attr" - {
        "can be set to true" {
            val config = XmlParser.parseConfigurationData("""<configuration scan="true" />""")
            config.scan shouldBe true
        }

        "can be set to false" {
            val config = XmlParser.parseConfigurationData("""<configuration scan="false" />""")
            config.scan shouldBe false
        }

        "is falsy by default" {
            val config = XmlParser.parseConfigurationData("""<configuration />""")
            config.scan shouldNotBe true
        }
    }

    "scanPeriod attr" - {
        "can be set" {
            val config = XmlParser.parseConfigurationData("""<configuration scan="true" scanPeriod="1 day" />""")
            config.scanPeriod shouldBe "1 day"
        }

        "is null by default" {
            val config = XmlParser.parseConfigurationData("""<configuration />""")
            config.scanPeriod should beNull()
        }
    }

    "property" - {
        "accepts system property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<property key="local1" value="value1" scope="system" />
                |</configuration>""".trimMargin())
            config.propertyMeta shouldNot beNull()
            config.propertyMeta!! shouldHaveSize 1
            config.propertyMeta!![0].key shouldBe "local1"
            config.propertyMeta!![0].value shouldBe "value1"
            config.propertyMeta!![0].scope shouldBe "system"
        }

        "accepts context property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<property key="local1" value="value1" scope="context" />
                |</configuration>""".trimMargin())
            config.propertyMeta shouldNot beNull()
            config.propertyMeta!! shouldHaveSize 1
            config.propertyMeta!![0].key shouldBe "local1"
            config.propertyMeta!![0].value shouldBe "value1"
            config.propertyMeta!![0].scope shouldBe "context"
        }

        "accepts local property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<property key="local1" value="value1" scope="local" />
                |</configuration>""".trimMargin())
            config.propertyMeta shouldNot beNull()
            config.propertyMeta!! shouldHaveSize 1
            config.propertyMeta!![0].key shouldBe "local1"
            config.propertyMeta!![0].value shouldBe "value1"
            config.propertyMeta!![0].scope shouldBe "local"
        }
    }

    "timestamp" - {
        "accepts system property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<timestamp key="local1" datePattern="yyyyMMdd" scope="system" />
                |</configuration>""".trimMargin())
            config.timestamps shouldNot beNull()
            config.timestamps!! shouldHaveSize 1
            config.timestamps!![0].key shouldBe "local1"
            config.timestamps!![0].datePattern shouldBe "yyyyMMdd"
            config.timestamps!![0].scope shouldBe "system"
        }

        "accepts context property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<timestamp key="local1" datePattern="yyyyMMdd" scope="context" />
                |</configuration>""".trimMargin())
            config.timestamps shouldNot beNull()
            config.timestamps!! shouldHaveSize 1
            config.timestamps!![0].key shouldBe "local1"
            config.timestamps!![0].datePattern shouldBe "yyyyMMdd"
            config.timestamps!![0].scope shouldBe "context"
        }

        "accepts local property" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<timestamp key="local1" datePattern="yyyyMMdd" scope="local" />
                |</configuration>""".trimMargin())
            config.timestamps shouldNot beNull()
            config.timestamps!! shouldHaveSize 1
            config.timestamps!![0].key shouldBe "local1"
            config.timestamps!![0].datePattern shouldBe "yyyyMMdd"
            config.timestamps!![0].scope shouldBe "local"
        }
    }

    "include" - {
        "sets value" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<include file="/path/to/foo.xml" />
                |</configuration>""".trimMargin())

            config.includes!! shouldHaveSize 1
            config.includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<include file="/path/to/foo.xml" />
                |<include file="/path/to/bar.xml" />
                |</configuration>""".trimMargin())

            config.includes!! shouldHaveSize 2
            config.includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
            config.includes!!.find { it.file == "/path/to/bar.xml" } shouldNot beNull()
        }

        "optional include does not throw error" {
            shouldNotThrowAny {
                XmlParser.parseConfigurationData("""<configuration>
                |<include file="/path/to/nonexistent.xml" optional="true" />
                |</configuration>""".trimMargin())
            }
        }
    }

    "includes" - {
        "sets value" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<includes>
                |  <include file="/path/to/foo.xml" />
                |</includes>
                |</configuration>""".trimMargin())

            config.optionalIncludes!! shouldHaveSize 1
            config.optionalIncludes!![0].includes!!.find { it.file == "/path/to/foo.xml" } shouldNot beNull()
        }

        "sets values" {
            val config = XmlParser.parseConfigurationData("""<configuration>
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
        "sets value" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<appender name="myApp" class="com.example.MyAppender">
                |  <file>/path/to/foo.xml</file>
                |  <lazy>true</lazy>
                |</appender>
                |</configuration>""".trimMargin())

            config.appenderMeta!! shouldHaveSize 1
            config.appenderMeta!![0].name shouldBe "myApp"
            config.appenderMeta!![0].className shouldBe "com.example.MyAppender"
        }

        "sets values" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<appender name="myApp" class="com.example.MyAppender">
                |  <file>/path/to/foo.xml</file>
                |  <lazy>true</lazy>
                |</appender>
                |<appender name="myApp2" class="com.example.MyAppender2">
                |  <file>/path/to/foo.xml</file>
                |  <lazy>true</lazy>
                |</appender>
                |</configuration>""".trimMargin())

            config.appenderMeta!! shouldHaveSize 2
            config.appenderMeta!![0].name shouldBe "myApp"
            config.appenderMeta!![0].className shouldBe "com.example.MyAppender"
            config.appenderMeta!![1].name shouldBe "myApp2"
            config.appenderMeta!![1].className shouldBe "com.example.MyAppender2"
        }
    }

    "logger" - {
        "sets value" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<logger name="com.example.MyAppender" level="DEBUG" />
                |</configuration>""".trimMargin())

            config.loggers!! shouldHaveSize 1
            config.loggers!![0].name shouldBe "com.example.MyAppender"
            config.loggers!![0].level shouldBe "DEBUG"
        }

        "sets values" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<logger name="com.example.MyAppender" level="DEBUG" />
                |<logger name="com.example.MyAppender2" level="ERROR" />
                |</configuration>""".trimMargin())

            config.loggers!! shouldHaveSize 2
            config.loggers!![0].name shouldBe "com.example.MyAppender"
            config.loggers!![0].level shouldBe "DEBUG"
            config.loggers!![1].name shouldBe "com.example.MyAppender2"
            config.loggers!![1].level shouldBe "ERROR"
        }

        "sets additivity" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<logger name="com.example.MyAppender" level="DEBUG" />
                |<logger name="com.example.MyAppender" level="DEBUG" additivity="false" />
                |</configuration>""".trimMargin())

            config.loggers!! shouldHaveSize 2
            config.loggers!![1].additivity shouldBe false
        }
    }

    "root" - {
        "sets level" {
            val config = XmlParser.parseConfigurationData("""<configuration>
                |<root level="DEBUG" />
                |</configuration>""".trimMargin())

            config.root shouldNot beNull()
            config.root!!.level shouldBe "DEBUG"
        }
    }
})
