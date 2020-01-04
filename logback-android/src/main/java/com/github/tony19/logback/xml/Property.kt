package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Property(
    var key: String,
    var value: String,
    var scope: String?
) {
    companion object {
        fun xml(k: Konsumer): Property {
            k.checkCurrent("property")

            return Property(
                key = k.attributes.getValue("key"),
                value = k.attributes.getValue("value"),
                scope = k.attributes.getValueOpt("scope")
            )
        }
    }
}