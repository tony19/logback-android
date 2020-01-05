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