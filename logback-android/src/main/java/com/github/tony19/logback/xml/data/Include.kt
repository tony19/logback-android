package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Include(
    var file: String? = null,
    var resource: String? = null,
    var url: String? = null,
    var optional: Boolean? = null
) {
    companion object {
        fun xml(k: Konsumer): Include {
            k.checkCurrent("include")

            return Include(
                    file = k.attributes.getValueOpt("file"),
                    resource = k.attributes.getValueOpt("resource"),
                    url = k.attributes.getValueOpt("url"),
                    optional = k.attributes.getValueOpt("optional")?.toBoolean()
            )
        }
    }
}