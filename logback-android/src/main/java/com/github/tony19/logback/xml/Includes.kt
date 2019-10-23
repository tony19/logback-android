package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Includes(
    var includes: List<Include>? = emptyList()
) {
    companion object {
        fun xml(k: Konsumer): Includes {
            k.checkCurrent("includes")

            return Includes(
                includes = k.children("include") { Include.xml(this) }
            )
        }
    }
}