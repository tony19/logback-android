package ch.qos.logback.core

import ch.qos.logback.classic.Logger
import ch.qos.logback.core.dsl.Logback
import kotlin.reflect.full.companionObject

fun <R: Any> R.logger(): Lazy<Logger> {
    return lazy { Logback.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

// Return logger for Java class, if companion object fix the name
fun <T: Any> logger(forClass: Class<T>): Logger {
    return Logback.getLogger(unwrapCompanionClass(forClass).name)
}

// unwrap companion class to enclosing class given a Java Class
fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}
