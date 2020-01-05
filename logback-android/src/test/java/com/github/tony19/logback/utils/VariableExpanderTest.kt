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