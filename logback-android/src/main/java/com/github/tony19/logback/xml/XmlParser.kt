package com.github.tony19.logback.xml

import com.github.tony19.logback.xml.data.Configuration
import com.gitlab.mvysny.konsumexml.konsumeXml

class XmlParser {
    companion object {
        fun parse(xmlDoc: String, clock: IClock = SystemClock()): ConfigurationContext {
            return Configurator(ConfigurationContext(clock=clock)).configure { xmlDoc.konsumeXml() }
        }

        internal fun parseConfigurationData(xmlDoc: String) = xmlDoc.konsumeXml().child("configuration") { Configuration.xml(this) }
    }
}
