package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Timestamp(var key: String, var datePattern: String) {
    companion object {
        fun xml(k: Konsumer): Timestamp {
            k.checkCurrent("timestamp")

            return Timestamp(
                key = k.attributes.getValue("key"),
                datePattern = k.attributes.getValue("datePattern")
            )
        }
    }
}