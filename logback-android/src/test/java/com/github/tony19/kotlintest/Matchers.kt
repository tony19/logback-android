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