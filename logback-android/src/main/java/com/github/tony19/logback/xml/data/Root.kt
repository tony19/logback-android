package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Root(
    var level: String? = null,
    var appenderRefs: List<AppenderRef>
) {
    companion object {
        fun xml(k: Konsumer): Root {
            k.checkCurrent("root")

            return Root(
                    level = k.attributes.getValueOpt("level"),
                    appenderRefs = k.children("appender-ref") { AppenderRef.xml(this) }
            )
        }
    }
}
