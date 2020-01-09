package com.github.tony19.logback.xml

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.StatusListenerConfigHelper
import com.github.tony19.logback.utils.VariableExpander
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.konsumeXml
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class Configuration (
    var debug: Boolean? = false,
    var scan: Boolean? = false,
    var scanPeriod: String? = null,
    var appenderMeta: List<Appender>? = emptyList(),
    var propertyMeta: List<Property>? = emptyList(),
    var timestamps: List<Timestamp>? = emptyList(),
    var includes: List<Include>? = emptyList(),
    var optionalIncludes: List<Includes>?,
    var loggers: List<Logger>?,
    var root: Root?,
    var appenders: MutableList<ch.qos.logback.core.Appender<*>> = mutableListOf(),
    var properties: Properties,
    val context: LoggerContext,
    val clock: IClock
) {
    companion object {
        fun xml(xmlDoc: String, context: LoggerContext = LoggerContext(), clock: IClock = SystemClock()): Configuration {
            return resolveInPasses(context, Properties(), clock) { xmlDoc.konsumeXml() }
        }

        private fun resolveInPasses(context: LoggerContext, properties: Properties, clock: IClock, createStream: () -> Konsumer): Configuration {
            return createStream().use { k ->
                k.child("configuration") {
                    Configuration(
                            debug = attributes.getValueOpt("debug")?.toBoolean(),
                            scan = attributes.getValueOpt("scan")?.toBoolean(),
                            scanPeriod = attributes.getValueOpt("scanPeriod"),
                            appenderMeta = children("appender") { Appender.xml(this) },
                            propertyMeta = children("property") { Property.xml(this) },
                            timestamps = children("timestamp") { Timestamp.xml(this) },
                            includes = children("include") { Include.xml(this) },
                            optionalIncludes = children("includes") { Includes.xml(this) },
                            loggers = children("logger") { Logger.xml(this) },
                            root = childOpt("root") { Root.xml(this) },
                            properties = properties,
                            context = context,
                            clock = clock
                    )
                }
            }.apply {
                resolveDebug()
                resolveIncludes()
                resolveOptionalIncludes()
                resolveProperties()
                resolveTimestamps()

                val resolver = createValueResolver()
                createStream().use { k ->
                    k.child("configuration") {
                        resolveAppenders(this, resolver)
                        skipContents()
                    }
                }

                resolveLoggers()
                resolveScan()
            }
        }
    }

    private fun resolveScan() {
        scan?.let {
            println("warning: `scan` not supported at this time")
        }
        scanPeriod?.let {
            println("warning: `scan` not supported at this time")
        }
    }

    private fun createValueResolver() = XmlResolver { value ->
        if (value is String) {
            expandVar(value)

        } else {
            value.javaClass.methods.apply {
                find { it.name == "setContext" }?.invoke(value, context)
                find { it.name == "start" }?.invoke(value)
            }
            value
        }
    }

    private fun resolveDebug() {
        if (debug == true) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context, OnConsoleStatusListener())
        }
    }

    private fun resolveIncludes() {
        includes?.forEach {
            try {
                include(it)

            } catch (e: Exception) {
                val optional = it.optional ?: false
                if (!optional) {
                    throw e
                }
            }
        }
    }

    private fun include(include: Include) =
        when {
            !include.file.isNullOrEmpty() ->
                resolveInPasses(context, properties, clock) { File(include.file).konsumeXml() }

            !include.url.isNullOrEmpty() ->
                resolveInPasses(context, properties, clock) {
                    val connection = URL(include.url).openConnection()
                    // disable cache to prevent locking file unnecessarily
                    // when URL points to local file
                    connection.useCaches = false
                    connection.getInputStream().use { it.konsumeXml() }
                }

            !include.resource.isNullOrEmpty() && javaClass.classLoader !== null ->
                resolveInPasses(context, properties, clock) {
                    javaClass.classLoader!!.getResource(include.resource).openStream().use { it.konsumeXml() }
                }

            else -> null
        }

    private fun resolveOptionalIncludes() {
        optionalIncludes?.forEach { optionalInclude ->
            run loop@ {
                optionalInclude.includes?.forEach {
                    val result: Configuration? = try {
                        include(it)
                    } catch (e: Exception) {
                        null
                    }
                    if (result !== null) {
                        return@loop
                    }
                }
            }
        }
    }

    private fun resolveLoggers() {
        loggers?.forEach {
            val logger = context.getLogger(it.name)
            logger.level = Level.toLevel(it.level)
            logger.isAdditive = it.additivity?.toBoolean() ?: false
            it.appenderRefs.forEach { apdrRef ->
                appenders.find { apdr -> apdr.name == apdrRef.ref!! }?.let { appender ->
                    @Suppress("UNCHECKED_CAST")
                    logger.addAppender(appender as ch.qos.logback.core.Appender<ILoggingEvent>)
                }
            }
        }

        root?.let {
            val logger = context.getLogger(it.name ?: ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
            logger.level = Level.toLevel(it.level)
            it.appenderRefs.forEach { apdrRef ->
                appenders.find { apdr -> apdr.name === apdrRef.ref!! }?.let { appender ->
                    @Suppress("UNCHECKED_CAST")
                    logger.addAppender(appender as ch.qos.logback.core.Appender<ILoggingEvent>)
                }
            }
        }
    }

    private fun resolveTimestamps() {
        timestamps?.forEach {
            val time = if (it.timeRef == "contextBirth") context.birthTime else clock.currentTimeMillis()
            val date = SimpleDateFormat(it.datePattern, Locale.US).format(time)
            setProp(it.key, date, it.scope, ::expandVar)
        }

        // no need for timestamps anymore
        timestamps = null
    }

    private fun resolveProperties() {
        propertyMeta?.forEach {
            setProp(it.key, it.value, it.scope)
        }

        // second pass to expand any variables based on props
        propertyMeta?.forEach {
            setProp(it.key, it.value, it.scope, ::expandVar)
        }

        // no need for meta anymore
        propertyMeta = null
    }

    private fun setProp(key: String, value: String, scope: String?, valFn: (String) -> String = { it }) {
        when (scope?.toLowerCase(Locale.US) ?: "local") {
            "local" -> properties[key] = valFn(value)
            "system" -> System.setProperty(key, valFn(value))
            "context" -> context.putProperty(key, valFn(value))
        }
    }

    private fun expandVar(input: String) = VariableExpander().expand(input) {
        properties.getProperty(it) ?: context.getProperty(it) ?: System.getProperty(it) ?: System.getenv(it)
    }

    private fun resolveAppenders(k: Konsumer, resolver: IResolver) {
        if (appenderMeta?.isEmpty()!!) {
            System.err.println("no appenders defined")
            return
        }

        val (matchedAppenders, unknownAppenders) = getAppenderRefs().map { appenderName ->
            appenderMeta?.find { it.name == appenderName }
        }.partition { it !== null }

        // no need for meta anymore
        appenderMeta = null

        unknownAppenders.forEach {
            System.err.println("unknown appender ref: ${it!!.name}")
        }

        if (matchedAppenders.isEmpty()) {
            System.err.println("no referenced appenders")

        } else {
            val matchedAppenderNames = matchedAppenders.map { it!!.name!! }

            k.children("appender") {
                val name = attributes.getValue("name")
                val className = attributes.getValue("class")
                if (name in matchedAppenderNames) {
                    val newAppender = resolver.resolve<ch.qos.logback.core.Appender<*>>(this, className)
                    newAppender.name = name
                    newAppender.context = context
                    newAppender.start()
                    appenders.add(newAppender)
                }
            }
        }
    }

    private fun getAppenderRefs(): List<String> {
        val rootAppenderRefs = root?.appenderRefs?.map { it.ref!! }
        val loggerAppenderRefs = loggers?.flatMap { logger -> logger.appenderRefs.map { it.ref!! } }
        return (rootAppenderRefs ?: emptyList()) + (loggerAppenderRefs ?: emptyList())
    }
}