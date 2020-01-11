/*
 * Copyright (c) 2020 Anthony Trinh.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tony19.logback.xml.data

import com.gitlab.mvysny.konsumexml.Konsumer

data class Configuration (
        var debug: Boolean? = false,
        var scan: Boolean? = false,
        var scanPeriod: String? = null,
        var appenderMeta: List<Appender>? = emptyList(),
        var propertyMeta: List<Property>? = emptyList(),
        var timestamps: List<Timestamp>? = emptyList(),
        var includes: List<Include>? = emptyList(),
        var optionalIncludes: List<Includes>? = emptyList(),
        var loggers: List<Logger>? = emptyList(),
        var root: Root? = null
) {
    companion object {
        fun xml(k: Konsumer): Configuration {
            k.checkCurrent("configuration")
            return Configuration(
                    debug = k.attributes.getValueOpt("debug")?.toBoolean(),
                    scan = k.attributes.getValueOpt("scan")?.toBoolean(),
                    scanPeriod = k.attributes.getValueOpt("scanPeriod"),
                    appenderMeta = k.children("appender") { Appender.xml(this) },
                    propertyMeta = k.children("property") { Property.xml(this) },
                    timestamps = k.children("timestamp") { Timestamp.xml(this) },
                    includes = k.children("include") { Include.xml(this) },
                    optionalIncludes = k.children("includes") { Includes.xml(this) },
                    loggers = k.children("logger") { Logger.xml(this) },
                    root = k.childOpt("root") { Root.xml(this) }
            )
        }
    }
}
