package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Timestamp(var key: String, var datePattern: String, var timeRef: String?, var scope: String?) {
    companion object {
        fun xml(k: Konsumer): Timestamp {
            k.checkCurrent("timestamp")

            return Timestamp(
                    key = k.attributes.getValue("key"),
                    datePattern = k.attributes.getValue("datePattern"),
                    timeRef = k.attributes.getValueOpt("timeReference"),
                    scope = k.attributes.getValueOpt("scope")
            )
        }
    }
}