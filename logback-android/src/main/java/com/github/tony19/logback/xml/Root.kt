package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer

data class Root(
    var name: String? = "root",
    var level: String? = null
) {
    companion object {
        fun xml(k: Konsumer): Root {
            k.checkCurrent("root")

            return Root(
                name = k.attributes.getValueOpt("name"),
                level = k.attributes.getValueOpt("level")
            )
        }
    }
}