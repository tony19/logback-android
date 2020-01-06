package com.github.tony19.logback.utils

import io.kotlintest.specs.FreeSpec
import io.kotlintest.shouldBe

class TextUtilsTest: FreeSpec({
    "capitalized" - {
        "capitalizes first letter" {
            "setFooBar".capitalized() shouldBe "SetFooBar"
        }

        "ignores already capitalized text" {
            "SetFooBar".capitalized() shouldBe "SetFooBar"
        }

        "ignores empty string" {
            "".capitalized() shouldBe ""
        }

        "ignores blank string" {
            "   ".capitalized() shouldBe "   "
        }
    }
})