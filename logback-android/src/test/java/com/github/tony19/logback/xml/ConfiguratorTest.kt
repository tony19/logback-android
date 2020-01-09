package com.github.tony19.logback.xml

import ch.qos.logback.core.status.OnConsoleStatusListener
import io.kotlintest.*
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.beNull
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.specs.FreeSpec
import io.kotlintest.extensions.system.withEnvironment
import io.kotlintest.extensions.system.withSystemProperties
import io.kotlintest.extensions.system.withSystemProperty
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

class ConfiguratorTest: FreeSpec({

    "debug attr" - {
        "adds a console status listener" {
            val context = XmlParser.parse("""<configuration debug="true"> </configuration>""")
            val listeners = context.loggerContext.statusManager.copyOfStatusListenerList
            listeners shouldHaveSize 1
            listeners[0].shouldBeInstanceOf<OnConsoleStatusListener>()
        }
    }

    "property" - {
        "sets system property" {
            val context = XmlParser.parse("""<configuration>
            |<property key="logback.test.foo" value="bar" scope="system" />
            |</configuration>""".trimMargin())
            System.getProperty("logback.test.foo") shouldBe "bar"
            context.loggerContext.getProperty("logback.test.foo") shouldBe null
            context.properties["logback.test.foo"] shouldBe null
        }

        "sets context property" {
            val context = XmlParser.parse("""<configuration>
            |<property key="myKey" value="myValue" scope="context" />
            |</configuration>""".trimMargin())
            context.loggerContext.getProperty("myKey") shouldBe "myValue"
            context.properties["myKey"] shouldBe null
        }

        "sets local property" {
            val context = XmlParser.parse("""<configuration>
            |<property key="local1" value="value1" scope="local" />
            |<property key="local2" value="value2" />
            |</configuration>""".trimMargin())
            context.properties["local1"] shouldBe "value1"
            context.properties["local2"] shouldBe "value2"
            context.loggerContext.getProperty("local1") shouldBe null
            context.loggerContext.getProperty("local2") shouldBe null
        }

        "sets local property with variables" {
            val context = XmlParser.parse("""<configuration>
            |<property key="logdir" value="/path/to/logs" scope="local" />
            |<property key="logfile" value="${'$'}{logdir}/log.txt" />
            |</configuration>""".trimMargin())

            context.properties["logfile"] shouldBe "/path/to/logs/log.txt"
        }

        "reads system property as fallback" {
            withSystemProperty("FOO", "sysFooValue") {
                val context = XmlParser.parse("""<configuration>
                |<property key="myKey" value="${'$'}{FOO}" />
                |</configuration>""".trimMargin())

                context.properties["myKey"] shouldBe "sysFooValue"
            }
        }

        "reads environment variable as fallback" {
            withEnvironment("FOO", "envFooValue") {
                val context = XmlParser.parse("""<configuration>
                |<property key="myKey" value="${'$'}{FOO}" />
                |</configuration>""".trimMargin())

                context.properties["myKey"] shouldBe "envFooValue"
            }
        }

        "prefers system property as fallback" {
            withSystemProperty("FOO", "sysFooValue") {
                withEnvironment("FOO", "envFooValue") {
                    val context = XmlParser.parse("""<configuration>
                |<property key="myKey" value="${'$'}{FOO}" />
                |</configuration>""".trimMargin())

                    context.properties["myKey"] shouldBe "sysFooValue"
                }
            }
        }
    }

    "timestamp" - {
        val testClockInstant = Instant.parse("2020-01-23T12:34:56Z")
        val testClock = object: IClock {
            override fun currentTimeMillis() = testClockInstant.toEpochMilli()
        }

        fun test(scope: String, fn: (String?, String?, Any?) -> Unit) {
            withSystemProperties(mapOf()) {
                val scopeText = if (scope.isNullOrEmpty()) "" else "scope=\"${scope}\""
                val context = XmlParser.parse("""<configuration>
                    |<timestamp key="logback.testKey" datePattern="yyyyMMdd'T'HHmmss" ${scopeText}/>
                    |</configuration>""".trimMargin(), clock=testClock)

                fn(
                    System.getProperty("logback.testKey"),
                    context.loggerContext.getProperty("logback.testKey"),
                    context.properties["logback.testKey"]
                )
            }
        }

        "sets context property" {
            test("context") { sysProp, contextProp, localProp ->
                sysProp shouldBe null
                contextProp shouldBe SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date.from(testClockInstant))
                localProp shouldBe null
            }
        }

        "set system property" {
            test("system") { sysProp, contextProp, localProp ->
                sysProp shouldBe SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date.from(testClockInstant))
                contextProp shouldBe null
                localProp shouldBe null
            }
        }

        "set local property" {
            test("local") { sysProp, contextProp, localProp ->
                sysProp shouldBe null
                contextProp shouldBe null
                localProp shouldBe SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date.from(testClockInstant))
            }
        }

        "set local property with no scope specified" {
            test("") { sysProp, contextProp, localProp ->
                sysProp shouldBe null
                contextProp shouldBe null
                localProp shouldBe SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date.from(testClockInstant))
            }
        }
    }

    "include" - {
        "optional include does not throw error" {
            shouldNotThrowAny {
                XmlParser.parse("""<configuration>
                |<include file="/path/to/nonexistent.xml" optional="true" />
                |</configuration>""".trimMargin())
            }
        }
    }

    "includes" - {
        "does not throw errors if not found" {
            shouldNotThrowAny {
                XmlParser.parse("""<configuration>
                |<includes>
                |  <include file="/path/to/nonexistent.xml" />
                |  <include url="http://nonexistent__foo.html" />
                |  <include resource="bar.html" />
                |</includes>
                |</configuration>""".trimMargin())
            }
        }

        "takes first include found" {
            val tmpFile = tmpConfigFile("""<configuration>
                |<property key="foo" value="bar" />
                |</configuration>
            """.trimMargin())
            val tmpFile2 = tmpConfigFile("""<configuration>
                |<property key="foo" value="baz" />
                |</configuration>
            """.trimMargin())
            val config = XmlParser.parse("""<configuration>
                |<includes>
                |  <include file="/path/to/nonexistent1.xml" />
                |  <include file="/path/to/nonexistent2.xml" />
                |  <include file="${tmpFile.absolutePath}" />
                |  <include file="${tmpFile2.absolutePath}" />
                |</includes>
                |</configuration>""".trimMargin())

            config.properties["foo"] shouldBe "bar"
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
            val context = XmlParser.parse("""<configuration>
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

            val appender = context.appenders.find { it.name == "logcat" }

            appender shouldNot beNull()
            appender.shouldBeInstanceOf<ch.qos.logback.classic.android.LogcatAppender>()

            "and configures it" {
                (appender as ch.qos.logback.classic.android.LogcatAppender).apply {
                    tagEncoder shouldNot beNull()
                    encoder shouldNot beNull()
                    tagEncoder!!.pattern shouldBe "%logger{12}%nopex" // %nopex automatically appended
                    encoder!!.pattern shouldBe "[%-20thread] %msg"
                    isStarted shouldBe true
                }
            }
        }

        "creates FileAppender instance" - {
            val context = XmlParser.parse("""<configuration>
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

            val appender = context.appenders.find { it.name == "file" }

            appender shouldNot beNull()
            appender.shouldBeInstanceOf<ch.qos.logback.core.FileAppender<*>>()

            "and configures it" {
                (appender as ch.qos.logback.core.FileAppender<*>).apply {
                    encoder shouldNot beNull()
                    (encoder!! as ch.qos.logback.classic.encoder.PatternLayoutEncoder).pattern shouldBe "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{35} - %msg%n"
                    file shouldBe "/data/data/com.example/files/log.txt"
                    lazy shouldBe true
                    isStarted shouldBe true
                }
            }
        }
    }

    "logger" - {
        "creates logger" {
            val context = XmlParser.parse("""<configuration>
                |<appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
                |  <encoder>
                |    <pattern>[%-20thread] %msg</pattern>
                |  </encoder>
                |</appender>
                |<logger name="com.example.foo">
                |  <appender-ref ref="logcat" />
                |</logger>
                |</configuration>""".trimMargin())
            context.loggerContext.loggerList shouldHaveSize 4 // root, "com", "example", "foo"
            context.loggerContext.loggerList[3].apply {
                name shouldBe "com.example.foo"
                val appenderList = iteratorForAppenders().asSequence().toList()
                appenderList shouldHaveSize 1
                appenderList[0].shouldBeInstanceOf<ch.qos.logback.classic.android.LogcatAppender>()
                val logcat = appenderList[0] as ch.qos.logback.classic.android.LogcatAppender
                logcat.encoder?.pattern shouldNot beNull()
                logcat.name shouldBe "logcat"
            }
        }
    }
})

fun tmpConfigFile(config: String = "<configuration />") = createTempFile().apply {
    writeText(config)
    deleteOnExit()
}
