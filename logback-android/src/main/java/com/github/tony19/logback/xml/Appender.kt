package com.github.tony19.logback.xml

import ch.qos.logback.classic.spi.ILoggingEvent
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.anyName

data class Appender (
    var name: String? = null,
    var className: String,
    var resolved: ch.qos.logback.core.Appender<*>?
) {
    companion object {
        fun xml(k: Konsumer, resolve: Boolean = false): Appender {
            k.checkCurrent("appender")
            val name = k.attributes.getValue("name")
            val className = k.attributes.getValue("class")
            var resolved: ch.qos.logback.core.Appender<*>? = null
            if (resolve) {
                val clazz = Class.forName(className)
                @Suppress("UNCHECKED_CAST")
                resolved = (clazz.getDeclaredConstructor().newInstance() as ch.qos.logback.core.Appender<ILoggingEvent>).apply {
                    k.children(anyName) {
                        val method = clazz.methods.find { it.name == "set${k.name.toString().capitalize()}" && it.parameterCount == 1 }
                        method?.invoke(k.text())
                    }
                }
            } else {
                k.skipContents()
            }
            return Appender(name, className, resolved)
        }
    }
}