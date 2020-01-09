package com.github.tony19.logback.xml

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.util.StatusListenerConfigHelper
import com.github.tony19.logback.utils.VariableExpander
import com.github.tony19.logback.xml.data.*
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.konsumeXml
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Configurator(val context: ConfigurationContext = ConfigurationContext()) {

    fun configure(getXmlConfig: () -> Konsumer): ConfigurationContext {
        val config = getXmlConfig().use{ k ->
            k.child("configuration") { Configuration.xml(this) }
        }

        resolveDebug(config)
        resolveIncludes(config)
        resolveOptionalIncludes(config)
        resolveProperties(config)
        resolveTimestamps(config)

        val resolver = createValueResolver()
        getXmlConfig().use { k ->
            k.child("configuration") {
                resolveAppenders(config, this, resolver)
                skipContents()
            }
        }

        resolveLoggers(config)
        resolveScan(config)
        return context
    }

    private fun resolveScan(config: Configuration) {
        config.scan?.let {
            println("warning: `scan` not supported at this time")
        }
        config.scanPeriod?.let {
            println("warning: `scan` not supported at this time")
        }
    }

    private fun createValueResolver() = XmlDeserializer { value ->
        if (value is String) {
            expandVar(value)

        } else {
            value.javaClass.methods.apply {
                find { it.name == "setContext" }?.invoke(value, context.loggerContext)
                find { it.name == "start" }?.invoke(value)
            }
            value
        }
    }

    private fun resolveDebug(config: Configuration) {
        if (config.debug == true) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context.loggerContext, OnConsoleStatusListener())
        }
    }

    private fun resolveIncludes(config: Configuration) {
        config.includes?.forEach {
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
                    Configurator(context).configure { File(include.file).konsumeXml() }

                !include.url.isNullOrEmpty() ->
                    Configurator(context).configure {
                        val connection = URL(include.url).openConnection()
                        // disable cache to prevent locking file unnecessarily
                        // when URL points to local file
                        connection.useCaches = false
                        connection.getInputStream().use { it.konsumeXml() }
                    }

                !include.resource.isNullOrEmpty() && javaClass.classLoader !== null ->
                    Configurator(context).configure {
                        javaClass.classLoader!!.getResource(include.resource).openStream().use { it.konsumeXml() }
                    }

                else -> null
            }

    private fun resolveOptionalIncludes(config: Configuration) {
        config.optionalIncludes?.forEach { optionalInclude ->
            run loop@ {
                optionalInclude.includes?.forEach {
                    val result = try { include(it) } catch (e: Exception) { null }
                    if (result !== null) {
                        return@loop
                    }
                }
            }
        }
    }

    private fun resolveLoggers(config: Configuration) {
        config.loggers?.forEach {
            val logger = context.loggerContext.getLogger(it.name)
            logger.level = Level.toLevel(it.level)
            logger.isAdditive = it.additivity ?: false
            it.appenderRefs.forEach { apdrRef ->
                context.appenders.find { apdr -> apdr.name == apdrRef.ref!! }?.let { appender ->
                    @Suppress("UNCHECKED_CAST")
                    logger.addAppender(appender as ch.qos.logback.core.Appender<ILoggingEvent>)
                }
            }
        }

        config.root?.let {
            val logger = context.loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
            logger.level = Level.toLevel(it.level)
            it.appenderRefs.forEach { apdrRef ->
                context.appenders.find { apdr -> apdr.name === apdrRef.ref!! }?.let { appender ->
                    @Suppress("UNCHECKED_CAST")
                    logger.addAppender(appender as ch.qos.logback.core.Appender<ILoggingEvent>)
                }
            }
        }
    }

    private fun resolveTimestamps(config: Configuration) {
        config.timestamps?.forEach {
            val time = if (it.timeRef == "contextBirth") context.loggerContext.birthTime else context.clock.currentTimeMillis()
            val date = SimpleDateFormat(it.datePattern, Locale.US).format(time)
            setProp(it.key, date, it.scope, ::expandVar)
        }

        // no need for timestamps anymore
        config.timestamps = null
    }

    private fun resolveProperties(config: Configuration) {
        config.propertyMeta?.forEach {
            setProp(it.key, it.value, it.scope)
        }

        // second pass to expand any variables based on props
        config.propertyMeta?.forEach {
            setProp(it.key, it.value, it.scope, ::expandVar)
        }

        // no need for meta anymore
        config.propertyMeta = null
    }

    private fun setProp(key: String, value: String, scope: String?, valFn: (String) -> String = { it }) {
        when (scope?.toLowerCase(Locale.US) ?: "local") {
            "local" -> context.properties[key] = valFn(value)
            "system" -> System.setProperty(key, valFn(value))
            "context" -> context.loggerContext.putProperty(key, valFn(value))
        }
    }

    private fun expandVar(input: String) = VariableExpander().expand(input) {
        context.properties.getProperty(it) ?: context.loggerContext.getProperty(it) ?: System.getProperty(it) ?: System.getenv(it)
    }

    private fun resolveAppenders(config: Configuration, k: Konsumer, deserializer: IDeserializer) {
        if (config.appenderMeta?.isEmpty()!!) {
            System.err.println("no appenders defined")
            return
        }

        val (matchedAppenders, unknownAppenders) = getAppenderRefs(config).map { appenderName ->
            config.appenderMeta?.find { it.name == appenderName }
        }.partition { it !== null }

        // no need for meta anymore
        config.appenderMeta = null

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
                    val newAppender = deserializer.deserialize<ch.qos.logback.core.Appender<*>>(this, className)
                    newAppender.name = name
                    newAppender.context = context.loggerContext
                    newAppender.start()
                    context.appenders.add(newAppender)
                }
            }
        }
    }

    private fun getAppenderRefs(config: Configuration): List<String> {
        val rootAppenderRefs = config.root?.appenderRefs?.map { it.ref!! }
        val loggerAppenderRefs = config.loggers?.flatMap { logger -> logger.appenderRefs.map { it.ref!! } }
        return (rootAppenderRefs ?: emptyList()) + (loggerAppenderRefs ?: emptyList())
    }
}
