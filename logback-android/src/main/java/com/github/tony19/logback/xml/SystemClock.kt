package com.github.tony19.logback.xml

internal class SystemClock: IClock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}