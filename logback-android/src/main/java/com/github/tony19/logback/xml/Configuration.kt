package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.konsumeXml
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
    var properties: Properties = Properties()
) {
    companion object {
        fun xml(xmlDoc: String): Configuration {
            return xmlDoc.konsumeXml().use { k ->
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
                            root = childOpt("root") { Root.xml(this) }
                    )
                }
            }.apply {
                resolveProperties()

                xmlDoc.konsumeXml().use { k ->
                    k.child("configuration") {
                        resolveAppenders(this, XmlResolver())
                        skipContents()
                    }
                }
            }
        }
    }

    private fun resolveProperties() {
        // local properties
        propertyMeta?.forEach {
            properties[it.key] = it.value
        }

        // no need for meta anymore
        propertyMeta = null
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