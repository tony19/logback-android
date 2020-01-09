package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

interface IDeserializer {
    fun <T> deserialize(k: Konsumer, className: String): T
    fun deserialize(k: Konsumer, inst: Any): Any
}