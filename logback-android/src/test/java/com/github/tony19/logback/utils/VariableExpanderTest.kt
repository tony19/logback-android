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

import io.kotlintest.specs.FreeSpec
import io.kotlintest.shouldBe

class VariableExpanderTest: FreeSpec({
    "uses specified delimeters" {
        VariableExpander("#`", "`").expand("hello #`name:-#`otherName``", mapOf("otherName" to "bob")) shouldBe "hello bob"
    }

    "expands variable" {
        VariableExpander().expand("hello ${'$'}{name}", mapOf("name" to "john")) shouldBe "hello john"
    }

    "expands nested variable" {
        VariableExpander().expand("hello ${'$'}{name:-${'$'}{otherName}}", mapOf("otherName" to "bob")) shouldBe "hello bob"
    }

    "expands deep nested variable" {
        VariableExpander().expand("hello ${'$'}{name:-to everyone in ${'$'}{city:-${'$'}{state}}}!!", mapOf("state" to "NY")) shouldBe "hello to everyone in NY!!"
    }

    "gets fallback variable" {
        VariableExpander().expand("hello ${'$'}{name:-world}", mapOf()) shouldBe "hello world"
    }
})