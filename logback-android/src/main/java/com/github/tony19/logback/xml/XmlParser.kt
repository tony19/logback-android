package com.github.tony19.logback.xml

class XmlParser {
    companion object {
        fun parse(xml: String) = Configuration.xml(xml)
    }
}