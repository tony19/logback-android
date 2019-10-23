package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.konsumeXml

class XmlParser {
    companion object {
        fun parse(xml: String) = xml.konsumeXml().use { k -> k.child("configuration") { Configuration.xml(this) }}
    }
}