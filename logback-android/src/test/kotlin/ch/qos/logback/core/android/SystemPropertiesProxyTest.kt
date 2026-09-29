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

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests the [SystemPropertiesProxy] class
 *
 * Values of the real `android.os.SystemProperties` are set with Robolectric's
 * [ShadowSystemProperties], which resets them after each test. The other
 * cases load a fake `android.os.SystemProperties` from a custom class loader.
 * Each test uses a fresh proxy (from the private constructor) so that the
 * shared singleton, which `OptionHelper` uses, is never reconfigured.
 */
@RunWith(RobolectricTestRunner::class)
class SystemPropertiesProxyTest {

    @Test
    fun `getInstance returns one shared proxy of the Android system properties`() {
        ShadowSystemProperties.override(KEY, "hello")

        val proxy = SystemPropertiesProxy.getInstance()

        assertSame(proxy, SystemPropertiesProxy.getInstance())
        assertEquals("hello", proxy.get(KEY, "fallback"))
        assertEquals("fallback", proxy.get(MISSING_KEY, "fallback"))
    }

    @Test
    fun `get returns the value of a system property`() {
        ShadowSystemProperties.override(KEY, "hello")

        assertEquals("hello", newProxy(null).get(KEY, "fallback"))
    }

    @Test
    fun `get returns the default for a missing system property`() {
        val proxy = newProxy(null)

        assertEquals("fallback", proxy.get(MISSING_KEY, "fallback"))
        assertNull(proxy.get(MISSING_KEY, null))
    }

    @Test
    fun `get returns the default for an empty system property`() {
        ShadowSystemProperties.override(KEY, "")

        assertEquals("fallback", newProxy(null).get(KEY, "fallback"))
    }

    @Test
    fun `getBoolean parses true values`() {
        val proxy = newProxy(null)
        for (value in listOf("1", "y", "yes", "on", "true")) {
            ShadowSystemProperties.override(KEY, value)

            assertEquals(true, proxy.getBoolean(KEY, false), value)
        }
    }

    @Test
    fun `getBoolean parses false values`() {
        val proxy = newProxy(null)
        for (value in listOf("0", "n", "no", "off", "false")) {
            ShadowSystemProperties.override(KEY, value)

            assertEquals(false, proxy.getBoolean(KEY, true), value)
        }
    }

    @Test
    fun `getBoolean returns the default for a missing or unparsable system property`() {
        val proxy = newProxy(null)
        ShadowSystemProperties.override(KEY, "maybe")

        for (def in listOf(true, false)) {
            assertEquals(def, proxy.getBoolean(MISSING_KEY, def))
            assertEquals(def, proxy.getBoolean(KEY, def))
        }
    }

    @Test
    fun `a null key falls back to the default`() {
        // SystemProperties rejects a null key with a NullPointerException,
        // which is swallowed like any other exception it throws
        val proxy = newProxy(null)

        assertEquals("fallback", proxy.get(null, "fallback"))
        for (def in listOf(true, false)) {
            assertEquals(def, proxy.getBoolean(null, def))
        }
    }

    @Test
    fun `setClassLoader looks up SystemProperties in the given class loader`() {
        val proxy = newProxy(null)

        proxy.setClassLoader(FakeSystemPropertiesLoader(EchoSystemProperties::class))

        assertEquals("echo:$KEY:fallback", proxy.get(KEY, "fallback"))
        assertEquals(false, proxy.getBoolean(KEY, true))
    }

    @Test
    fun `setClassLoader with null uses the class loader of the proxy`() {
        val proxy = proxyFor(EchoSystemProperties::class)
        ShadowSystemProperties.override(KEY, "yes")
        assertEquals("echo:$KEY:fallback", proxy.get(KEY, "fallback"))
        assertEquals(false, proxy.getBoolean(KEY, true))

        proxy.setClassLoader(null)

        assertEquals("yes", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
    }

    @Test
    fun `setClassLoader throws when the class loader has no SystemProperties`() {
        val proxy = newProxy(null)
        ShadowSystemProperties.override(KEY, "hello")

        assertFailsWith<ClassNotFoundException> {
            proxy.setClassLoader(FakeSystemPropertiesLoader(null))
        }
        // the previously loaded SystemProperties is still used
        assertEquals("hello", proxy.get(KEY, "fallback"))
    }

    @Test
    fun `setClassLoader throws when SystemProperties lacks get or getBoolean`() {
        // setClassLoader isn't atomic: each method is replaced once it is found
        val proxy = newProxy(null)
        ShadowSystemProperties.override(KEY, "yes")

        assertFailsWith<NoSuchMethodException> {
            proxy.setClassLoader(FakeSystemPropertiesLoader(NoMethodsSystemProperties::class))
        }
        assertEquals("yes", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, false))

        assertFailsWith<NoSuchMethodException> {
            proxy.setClassLoader(FakeSystemPropertiesLoader(StringOnlySystemProperties::class))
        }
        assertEquals("string-only:$KEY", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `proxy without SystemProperties returns null from get and the default from getBoolean`() {
        // the constructor swallows the ClassNotFoundException
        val proxy = newProxy(FakeSystemPropertiesLoader(null))

        assertNull(proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
        assertEquals(false, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `proxy whose SystemProperties lacks get and getBoolean returns null and the default`() {
        val proxy = proxyFor(NoMethodsSystemProperties::class)

        assertNull(proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
        assertEquals(false, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `proxy whose SystemProperties lacks getBoolean still reads strings`() {
        val proxy = proxyFor(StringOnlySystemProperties::class)

        assertEquals("string-only:$KEY", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
        assertEquals(false, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `exceptions thrown by SystemProperties fall back to the default`() {
        // even an IllegalArgumentException (e.g., for a key that is too long)
        // is swallowed, since Method#invoke wraps it
        val proxy = proxyFor(ThrowingSystemProperties::class)

        assertEquals("fallback", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
        assertEquals(false, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `get returns the default when SystemProperties returns null or an empty string`() {
        assertEquals("fallback", proxyFor(NullSystemProperties::class).get(KEY, "fallback"))
        assertEquals("fallback", proxyFor(EmptySystemProperties::class).get(KEY, "fallback"))
        assertNull(proxyFor(EmptySystemProperties::class).get(KEY, null))
    }

    @Test
    fun `results of the wrong type fall back to the default`() {
        val proxy = proxyFor(MistypedSystemProperties::class)

        assertEquals("fallback", proxy.get(KEY, "fallback"))
        assertEquals(true, proxy.getBoolean(KEY, true))
        assertEquals(false, proxy.getBoolean(KEY, false))
    }

    @Test
    fun `getBoolean returns null when SystemProperties#getBoolean returns null`() {
        // getBoolean() returns a java.lang.Boolean and passes the null through
        // instead of falling back to the default
        assertNull(proxyFor(NullSystemProperties::class).getBoolean(KEY, true))
    }

    @Test
    fun `get and getBoolean throw IllegalArgumentException when SystemProperties has instance methods`() {
        // SystemProperties' methods are invoked on the Class object itself,
        // which Method#invoke rejects for instance methods
        val proxy = proxyFor(InstanceMethodSystemProperties::class)

        assertFailsWith<IllegalArgumentException> { proxy.get(KEY, "fallback") }
        assertFailsWith<IllegalArgumentException> { proxy.getBoolean(KEY, true) }
    }

    /**
     * Serves [systemProperties] as `android.os.SystemProperties`, or throws
     * [ClassNotFoundException] for it when that is null.
     */
    private class FakeSystemPropertiesLoader(private val systemProperties: KClass<*>?) :
        ClassLoader(FakeSystemPropertiesLoader::class.java.classLoader) {

        override fun loadClass(name: String): Class<*> = when {
            name != SYSTEM_PROPERTIES -> super.loadClass(name)
            systemProperties == null -> throw ClassNotFoundException(name)
            else -> systemProperties.java
        }
    }

    private object NoMethodsSystemProperties

    private object StringOnlySystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): String = "string-only:$key"
    }

    private object EchoSystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): String = "echo:$key:$def"

        @JvmStatic
        fun getBoolean(key: String?, def: Boolean): Boolean = !def
    }

    private object ThrowingSystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): String = throw IllegalArgumentException("key too long: $key")

        @JvmStatic
        fun getBoolean(key: String?, def: Boolean): Boolean = throw IllegalArgumentException("key too long: $key")
    }

    private object NullSystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): String? = null

        @JvmStatic
        fun getBoolean(key: String?, def: Boolean): Boolean? = null
    }

    private object EmptySystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): String = ""

        @JvmStatic
        fun getBoolean(key: String?, def: Boolean): Boolean = def
    }

    private object MistypedSystemProperties {
        @JvmStatic
        fun get(key: String?, def: String?): Any = 42

        @JvmStatic
        fun getBoolean(key: String?, def: Boolean): Any = "true" // not a Boolean
    }

    private class InstanceMethodSystemProperties {
        fun get(key: String?, def: String?): String? = def

        fun getBoolean(key: String?, def: Boolean): Boolean = def
    }

    private companion object {
        const val SYSTEM_PROPERTIES = "android.os.SystemProperties"
        const val KEY = "debug.logback.test"
        const val MISSING_KEY = "debug.logback.missing"

        /** Creates a proxy with the private constructor, like the singleton's. */
        fun newProxy(classLoader: ClassLoader?): SystemPropertiesProxy =
            SystemPropertiesProxy::class.java.getDeclaredConstructor(ClassLoader::class.java)
                .apply { isAccessible = true }
                .newInstance(classLoader)

        fun proxyFor(systemProperties: KClass<*>): SystemPropertiesProxy =
            newProxy(FakeSystemPropertiesLoader(systemProperties))
    }
}
