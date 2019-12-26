package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class AppenderRef (
    var ref: String? = null
) {
    companion object {
        fun xml(k: Konsumer): AppenderRef {
            k.checkCurrent("appender-ref")
            return AppenderRef(ref = k.attributes.getValue("ref"))
        }
    }
}