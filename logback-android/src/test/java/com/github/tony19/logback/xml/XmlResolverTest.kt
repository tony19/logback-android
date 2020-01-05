package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.konsumeXml
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class XmlResolverTest: FreeSpec({
    "sets primitive property by child tag" - {
        data class Dummy(var stringVal: String = "",
                         var byteVal: Byte = 0,
                         var intVal: Int = 0,
                         var boolVal: Boolean = false,
                         var shortVal: Short = 0,
                         var longVal: Long = 0,
                         var floatVal: Float = 0.00F,
                         var doubleVal: Double = 0.00,
                         var charsetVal: Charset = StandardCharsets.UTF_8
                        )

        val xmlDoc = """<doc>
            |  <stringVal>bob</stringVal>
            |  <byteVal>9</byteVal>
            |  <intVal>23</intVal>
            |  <boolVal>true</boolVal>
            |  <shortVal>160</shortVal>
            |  <longVal>1577785250150</longVal>
            |  <floatVal>99.00</floatVal>
            |  <doubleVal>123.5e10</doubleVal>
            |  <charsetVal>UTF-16BE</charsetVal>
            |</doc>
        """.trimMargin()

        fun resolve(fn: Dummy.() -> Unit) {
            xmlDoc.konsumeXml().child("doc") {
                fn(XmlResolver().resolve(this, Dummy()) as Dummy)
            }
        }

        "String" { resolve { stringVal shouldBe "bob" } }
        "Byte" { resolve { byteVal.compareTo(9) shouldBe 0 } }
        "Int" { resolve { intVal shouldBe 23 } }
        "Boolean" { resolve { boolVal shouldBe true } }
        "Short" { resolve { shortVal.compareTo(160) shouldBe 0 } }
        "Long" { resolve { longVal shouldBe 1577785250150 } }
        "Float" { resolve { floatVal shouldBe 99.00F } }
        "Double" { resolve { doubleVal shouldBe 123.5e10 } }
        "Charset" { resolve { charsetVal shouldBe StandardCharsets.UTF_16BE } }
    }

    "sets subclassed property" {
        open class Animal(var says: String = "")
        class Dog(var color: String): Animal("woof") {
            constructor(): this("brown") // explicit default constructor required by resolver
        }
        class Dummy(var pet: Animal = Animal())

        val xmlDoc = """<doc>
            |  <pet class="${Dog::class.java.name}">
            |    <says>grrrr</says>
            |    <color>tan</color>
            |  </pet>
            |</doc>
        """.trimMargin()

        xmlDoc.konsumeXml().child("doc") {
            val dummy = XmlResolver().resolve(this, Dummy()) as Dummy
            dummy.pet.says shouldBe "grrrr"
            (dummy.pet as Dog).color shouldBe "tan"
        }
    }

    "adds array items" {
        data class Dummy(var stringVal: Array<String> = arrayOf("a", "b")) {
            fun addStringVal(value: String) { // explicit adder method required by resolver
                stringVal += value
            }
        }

        val xmlDoc = """<doc>
            |  <stringVal>x</stringVal>
            |  <stringVal>y</stringVal>
            |  <stringVal>z</stringVal>
            |</doc>
        """.trimMargin()

        xmlDoc.konsumeXml().child("doc") {
            val dummy = XmlResolver().resolve(this, Dummy()) as Dummy
            dummy.stringVal shouldContainExactlyInAnyOrder arrayOf("a", "b", "x", "y", "z")
        }
    }

    "ignores value when missing adder for array items" {
        data class Dummy(var stringVal: Array<String> = arrayOf("a", "b"))

        val xmlDoc = """<doc>
            |  <stringVal>x</stringVal>
            |  <stringVal>y</stringVal>
            |  <stringVal>z</stringVal>
            |</doc>
        """.trimMargin()

        xmlDoc.konsumeXml().child("doc") {
            val dummy = XmlResolver().resolve(this, Dummy()) as Dummy
            dummy.stringVal shouldContainExactlyInAnyOrder arrayOf("a", "b")
        }
    }

    "does not set readonly property" {
        data class Dummy(val stringVal: String = "initial")

        val xmlDoc = """<doc>
            |  <stringVal>bob</stringVal>
            |</doc>
        """.trimMargin()

        xmlDoc.konsumeXml().child("doc") {
            val dummy = XmlResolver().resolve(this, Dummy()) as Dummy
            dummy.stringVal shouldBe "initial"
        }
    }

    "calls value handler" {
        data class Dummy(var stringVal: String = "initial")

        val xmlDoc = """<doc>
            |  <stringVal>bob</stringVal>
            |</doc>
        """.trimMargin()

        xmlDoc.konsumeXml().child("doc") {
            val onValue: (Any) -> Any = mock {
                onGeneric { invoke(any()) } doReturn ""
            }
            XmlResolver(onValue).resolve(this, Dummy()) as Dummy
            verify(onValue).invoke("bob")
        }
    }
})
