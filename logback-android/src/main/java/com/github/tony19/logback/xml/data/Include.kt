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