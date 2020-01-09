package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Appender (
    var name: String? = null,
    var className: String
) {
    companion object {
        fun xml(k: Konsumer): Appender {
            k.checkCurrent("appender")
            val name = k.attributes.getValue("name")
            val className = k.attributes.getValue("class")
            k.skipContents()
            return Appender(name, className)
        }
    }
}