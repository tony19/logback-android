package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.anyName
import java.nio.charset.Charset
import java.util.*

class XmlResolver {
    fun <T> resolve(k: Konsumer, className: String): T {
        val inst = Class.forName(className).getDeclaredConstructor().newInstance()
        @Suppress("UNCHECKED_CAST")
        return resolveAny(k, inst) as T
    }

    private fun resolveAny(k: Konsumer, inst: Any): Any {
        return inst.apply {
            val instMethods = inst.javaClass.methods
            k.children(anyName) {
                if (name?.localPart?.isNotEmpty()!!) {
                    val elemName = name?.localPart?.toLowerCase(Locale.US)
                    instMethods
                            .find {
                                it.name.toLowerCase(Locale.US) in arrayOf("set${elemName}", "add${elemName}")
                                        && it.parameterTypes.size == 1 }
                            ?.let { setterMethod ->
                                val paramType = setterMethod.parameterTypes[0]
                                val value = when {
                                    paramType.name == "java.lang.String" -> text()
                                    paramType.name == "java.lang.Charset" -> text { Charset.forName(it) }
                                    paramType.isPrimitive -> text { convertString(paramType, it)!! }
                                    else -> {
                                        val paramInst = paramType.getDeclaredConstructor().newInstance()
                                        resolveAny(this, paramInst).apply {
                                            // FIXME: We need to have LoggerContext set before calling start()
                                            //javaClass.methods.find { it.name == "start" }?.invoke(this)
                                        }
                                    }
                                }
                                setterMethod.invoke(inst, value)
                            }
                }
            }
        }
    }

    private fun convertString(paramType: Class<*>, rawValue: String): Any? {
        val paramTypeName = paramType.name.toLowerCase(Locale.US)
        return String::class.java.methods
                .find { it.name.toLowerCase(Locale.US) == "to${paramTypeName}" }
                ?.invoke(rawValue)
    }
}