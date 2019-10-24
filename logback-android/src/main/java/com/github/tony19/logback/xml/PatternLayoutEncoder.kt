package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class PatternLayoutEncoder (
    var pattern: String
) {
    companion object {
        fun xml(k: Konsumer): PatternLayoutEncoder {
            k.checkCurrent("encoder")
            return PatternLayoutEncoder(k.childText("pattern"))
        }
    }
}