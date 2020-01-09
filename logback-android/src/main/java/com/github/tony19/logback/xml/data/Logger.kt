package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Logger (
        var name: String,
        var level: String? = null,
        var appenderRefs: List<AppenderRef>,
        var additivity: Boolean?
) {
    companion object {
        fun xml(k: Konsumer): Logger {
            k.checkCurrent("logger")

            return Logger(
                    name = k.attributes.getValue("name"),
                    level = k.attributes.getValueOpt("level"),
                    additivity = k.attributes.getValueOpt("additivity")?.toBoolean(),
                    appenderRefs = k.children("appender-ref") { AppenderRef.xml(this) }
            )
        }
    }
}