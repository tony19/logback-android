package com.github.tony19.logback.xml

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.anyName
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.*

class XmlResolver(val onValue: (String) -> String = { it }): IResolver {
    override fun <T> resolve(k: Konsumer, className: String): T {
        @Suppress("UNCHECKED_CAST")
        return resolve(k, create(Class.forName(className))) as T
    }

    override fun resolve(k: Konsumer, inst: Any): Any {
        return inst.apply {
            val instMethods by lazy { inst.javaClass.methods }
            k.children(anyName) {
                if (name?.localPart?.isNotEmpty()!!) {
                    val setterMethod = findSetterMethod(instMethods, name?.localPart)
                    if (setterMethod == null) {
                        skipContents()
                        println("warning: setter method not found: \"set${name!!.localPart.capitalize()}\" or \"add${name!!.localPart.capitalize()}\"")

                    // Arrays require an adder method to insert values!
                    // (we don't support array initialization)
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
            paramType == java.lang.String::class.java -> onValue(k.text())
            paramType == java.nio.charset.Charset::class.java -> k.text { Charset.forName(onValue(it)) }
            paramType.isPrimitive -> k.text { parsePrimitive(paramType, onValue(it))!! }
            else -> {
                val className = k.attributes.getValueOpt("class")
                val param = getParamClass(className, paramType)

                resolve(k, create(param)).apply {
                    // FIXME: We need to have LoggerContext set before calling start()
                    //javaClass.methods.find { it.name == "start" }?.invoke(this)
                }
            }
        }
    }

    private fun create(clazz: Class<*>): Any = try {
        clazz.getDeclaredConstructor().newInstance()
    } catch(e: NoSuchMethodException) {
        println("warning: missing default constructor for \"${clazz.name}\"")
        throw e
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

    private fun getParamClass(className: String?, paramType: Class<*>): Class<*> {
        if (paramType.isInterface && className.isNullOrEmpty()) {
            return when (paramType) {
                ch.qos.logback.core.encoder.Encoder::class.java -> PatternLayoutEncoder::class.java
                ch.qos.logback.core.Layout::class.java -> PatternLayout::class.java
                else -> throw Error("warning: cannot instantiate interface: ${paramType.name}")
            }
        }
        return when {
            !className.isNullOrEmpty() -> Class.forName(className)
            else -> paramType
        }
    }
}