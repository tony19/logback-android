/**
 * Copyright 2019 Anthony Trinh
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
package ch.qos.logback.core.android

import java.lang.reflect.Method

/**
 * A proxy to get Android's global system properties (as opposed
 * to the default process-level system properties). Settings from
 * `adb setprop` can be accessed from this class.
 */
public class SystemPropertiesProxy private constructor(cl: ClassLoader?) {

    private var systemProperties: Class<*>? = null
    private var getStringMethod: Method? = null
    private var getBooleanMethod: Method? = null

    init {
        try {
            setClassLoader(cl)
        } catch (e: Exception) {
            // reflection failure results in no-op proxy
        }
    }

    /**
     * Sets the classloader to lookup the class for android.os.SystemProperties
     *
     * @param cl desired classloader
     * @throws ClassNotFoundException android.os.SystemProperties class not found
     * @throws SecurityException security manager does not allow class loading
     * @throws NoSuchMethodException get/getBoolean method does not exist
     */
    @Throws(ClassNotFoundException::class, SecurityException::class, NoSuchMethodException::class)
    public fun setClassLoader(cl: ClassLoader?) {
        val classLoader = cl ?: javaClass.classLoader
        val clazz = classLoader.loadClass("android.os.SystemProperties")
        systemProperties = clazz
        getStringMethod = clazz.getMethod("get", String::class.java, String::class.java)
        getBooleanMethod = clazz.getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
    }

    /**
     * Get the value for the given key in the Android system properties
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the property value; the default if the key isn't found;
     * or `null` if `android.os.SystemProperties` is inaccessible
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    public fun get(key: String, def: String?): String? {
        val method = getStringMethod ?: return null

        val ret = try {
            method.invoke(null, key, def) as? String
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            null
        }

        // if return value is null or empty, use the default
        // since neither of those are valid values
        return if (ret.isNullOrEmpty()) def else ret
    }

    /**
     * Get the value for the given key in the Android system properties,
     * returned as a boolean.
     *
     * Values 'n', 'no', '0', 'false' or 'off' are considered false. Values
     * 'y', 'yes', '1', 'true' or 'on' are considered true. (case insensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or
     * is not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    public fun getBoolean(key: String, def: Boolean): Boolean {
        val method = getBooleanMethod ?: return def

        return try {
            method.invoke(null, key, def) as? Boolean ?: def
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            def
        }
    }

    public companion object {
        private val SINGLETON = SystemPropertiesProxy(null)

        /**
         * Gets the singleton instance for this class
         *
         * @return the singleton
         */
        @JvmStatic
        public fun getInstance(): SystemPropertiesProxy = SINGLETON
    }
}
