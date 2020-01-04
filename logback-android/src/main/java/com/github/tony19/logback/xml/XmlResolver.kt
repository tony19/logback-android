package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.anyName
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.*

class XmlResolver {
    fun <T> resolve(k: Konsumer, className: String): T {
        val inst = Class.forName(className).getDeclaredConstructor().newInstance()
        @Suppress("UNCHECKED_CAST")
        return resolve(k, inst) as T
    }

    fun resolve(k: Konsumer, inst: Any): Any {
        return inst.apply {
            val instMethods = inst.javaClass.methods
            k.children(anyName) {
                if (name?.localPart?.isNotEmpty()!!) {
                    val setterMethod = findSetterMethod(instMethods, name?.localPart)
                    if (setterMethod == null) {
                        skipContents()
                        println("warning: setter method not found: \"set${name!!.localPart.capitalize()}\" or \"add${name!!.localPart.capitalize()}\"")

                    // Arrays require an adder method to insert values!
                    // (we don't support setting array items)
                    } else if (setterMethod.parameterTypes[0].isArray && setterMethod.name.startsWith("set")) {
                        skipContents()
                        println("warning: adder method not found: \"add${name!!.localPart.capitalize()}\"")

                    } else {
                        val value = resolveValue(this, setterMethod.parameterTypes[0])
                        setterMethod.invoke(inst, value)
                    }
                }
            }
        }
    }

    private fun findSetterMethod(instMethods: Array<Method>, elemLocalPartName: String?): Method? {
        val elemName = elemLocalPartName?.toLowerCase(Locale.US)
        val find: (String) -> Method? = { prefix ->
            instMethods.find {
                it.name.toLowerCase(Locale.US) == "${prefix}${elemName}"
                && it.parameterTypes.size == 1
            }
        }
        // Prioritize adder in case a setter exists for an array. The adder
        // adds a single param, which works with the flow in resolveValue().
        return find("add") ?: find("set")
    }

    private fun resolveValue(k: Konsumer, paramType: Class<*>): Any {
        return when {
            paramType.name == "java.lang.String" -> k.text()
            paramType.name == "java.nio.charset.Charset" -> k.text { Charset.forName(it) }
            paramType.isPrimitive -> k.text { parsePrimitive(paramType, it)!! }
            else -> {
                val className = k.attributes.getValueOpt("class")
                val param = if (className != null && className.isNotEmpty()) Class.forName(className) else paramType

                val paramInst = try {
                    param.getDeclaredConstructor().newInstance()
                } catch(e: NoSuchMethodException) {
                    println("warning: missing default constructor for \"${param.name}\"")
                    throw e
                }

                resolve(k, paramInst).apply {
                    // FIXME: We need to have LoggerContext set before calling start()
                    //javaClass.methods.find { it.name == "start" }?.invoke(this)
                }
            }
        }
    }

    private val stringConverters by lazy {
        mapOf(
                "string" to String::toString,
                "byte" to String::toByte,
                "int" to String::toInt,
                "short" to String::toShort,
                "long" to String::toLong,
                "float" to String::toFloat,
                "double" to String::toDouble,
                "boolean" to String::toBoolean,
                "biginteger" to String::toBigInteger,
                "bigdecimal" to String::toBigDecimal
        )
    }

    private fun parsePrimitive(paramType: Class<*>, rawValue: String): Any? {
        return stringConverters[paramType.name.toLowerCase(Locale.US)]?.invoke(rawValue)
    }
}