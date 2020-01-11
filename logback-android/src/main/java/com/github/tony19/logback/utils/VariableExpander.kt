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
package com.github.tony19.logback.utils

class VariableExpander(delimeterStart: String = "\\$\\{",
                       delimeterEnd: String = "}") {

    private val PATTERN by lazy { "${delimeterStart}(.*?)(?::-(.*))?${delimeterEnd}".toRegex() }

    fun expand(input: String, lookup: (String) -> String?): String {
        return PATTERN.replace(input) {
            var result: String? = null
            if (it.groups[1] !== null) {
                val primaryVar = it.groups[1]?.value!!
                result = if (PATTERN.containsMatchIn(primaryVar)) expand(primaryVar, lookup) else lookup(primaryVar)
            }
            if (result.isNullOrEmpty() && it.groups[2] !== null) {
                val secondaryVar = it.groups[2]?.value!!
                result = if (PATTERN.containsMatchIn(secondaryVar)) expand(secondaryVar, lookup) else secondaryVar
            }
            if (result == null) {
                result = Regex.escape(it.groups[0]?.value!!)
            }
            result
        }
    }

    fun expand(input: String, props: java.util.Properties) = expand(input, props::getProperty)

    fun expand(input: String, props: Map<String, String>) = expand(input, props::get)
}