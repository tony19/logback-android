package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Configuration (
    var debug: Boolean? = false,
    var scan: Boolean? = false,
    var scanPeriod: String? = null,
    var appenderMeta: List<Appender>? = emptyList(),
    var properties: List<Property>? = emptyList(),
    var timestamps: List<Timestamp>? = emptyList(),
    var includes: List<Include>? = emptyList(),
    var optionalIncludes: List<Includes>?,
    var loggers: List<Logger>?,
    var root: Root?,
    var resolvedAppenders: MutableList<ch.qos.logback.core.Appender<*>> = mutableListOf()
) {
    companion object {
        fun xml(k: Konsumer): Configuration {
            k.checkCurrent("configuration")

            return Configuration(
                debug = k.attributes.getValueOpt("debug")?.toBoolean(),
                scan = k.attributes.getValueOpt("scan")?.toBoolean(),
                scanPeriod = k.attributes.getValueOpt("scanPeriod"),
                appenderMeta = k.children("appender") { Appender.xml(this) },
                properties = k.children("property") { Property.xml(this) },
                timestamps = k.children("timestamp") { Timestamp.xml(this) },
                includes = k.children("include") { Include.xml(this) },
                optionalIncludes = k.children("includes") { Includes.xml(this) },
                loggers = k.children("logger") { Logger.xml(this) },
                root = k.childOpt("root") { Root.xml(this) }
            ).apply {
                val (matchedAppenders, unknownAppenders) = getAppenderRefs().map { appenderName ->
                    appenderMeta?.find { it.name == appenderName }
                }.partition { it !== null }

                unknownAppenders.forEach {
                    System.err.println("unknown appender ref: ${it!!.name}")
                }

                val resolver = XmlResolver()
                val matchedAppenderNames = matchedAppenders.map { it!!.name!! }

                // FIXME: XML stream is already read at this point, so we can't re-read it.
                // A solution would be to re-instantiate Konsumer instance from string.
                // Is that more expensive that just parsing everything in one pass?
                k.children("appender") {
                    val meta = Appender.xml(this)
                    if (meta.name in matchedAppenderNames) {
                        resolvedAppenders.add(resolver.resolve(this, meta.className))
                    }
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