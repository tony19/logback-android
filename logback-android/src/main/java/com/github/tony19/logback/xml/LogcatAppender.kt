package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class LogcatAppender (
    var name: String,
    val tagEncoder: PatternLayoutEncoder?,
    val encoder: PatternLayoutEncoder?
) {
    companion object {
        fun xml(k: Konsumer): LogcatAppender {
            k.checkCurrent("appender")
            val name = k.attributes.getValue("name")

            return LogcatAppender(
                name,
                tagEncoder = k.child("tagEncoder") { PatternLayoutEncoder.xml(this) },
                encoder = k.child("emncoder") { PatternLayoutEncoder.xml(this) }
            )
        }
    }
}