package com.github.tony19.logback.utils

import java.util.*

fun String.capitalized(): String {
    return if (isNotEmpty() && this[0].isLowerCase()) substring(0, 1).toUpperCase(Locale.US) + substring(1) else this
}