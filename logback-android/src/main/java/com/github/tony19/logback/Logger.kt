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
package com.github.tony19.logback

import ch.qos.logback.classic.Logger
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
        ofClass.enclosingClass?.kotlin?.companionObject?.java == ofClass
    } ?: ofClass
}
