package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Logger (
    var name: String,
    var level: String? = null,
    var appenderRefs: List<AppenderRef>
) {
    companion object {
        fun xml(k: Konsumer): Logger {
            k.checkCurrent("logger")

            return Logger(
                name = k.attributes.getValue("name"),
                level = k.attributes.getValueOpt("level"),
                appenderRefs = k.children("appenderRef") { AppenderRef.xml(this) }
            )
        }
    }
}