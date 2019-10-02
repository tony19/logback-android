package com.github.tony19.kotlintest

import io.kotlintest.Matcher
import io.kotlintest.MatcherResult
import io.kotlintest.should

inline fun <reified T: Any, reified K: Any> haveElementOfType() = object : Matcher<List<K>> {
    override fun test(value: List<K>) = MatcherResult(
            value.any { it::class == T::class },
            "List should contain an element of type ${T::class}",
            "List should not contain an element of type ${T::class}"
        )
}

inline fun <reified T: Any, reified K: Any> List<K>.shouldHaveElementOfType() = this should haveElementOfType<T, K>()