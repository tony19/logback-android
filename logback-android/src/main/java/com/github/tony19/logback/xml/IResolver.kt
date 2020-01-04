package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

interface IResolver {
    fun <T> resolve(k: Konsumer, className: String): T
    fun resolve(k: Konsumer, inst: Any): Any
}