package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.konsumeXml
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

    "sets subclassed property" - {
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
})
