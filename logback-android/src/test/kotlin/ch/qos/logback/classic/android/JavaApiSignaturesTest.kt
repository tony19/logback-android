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
package ch.qos.logback.classic.android

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.android.AndroidContextUtil
import ch.qos.logback.core.android.SystemPropertiesProxy
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.joran.spi.DefaultClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import kotlin.test.assertEquals

/**
 * Pins the JVM-level API of the Android-specific classes, which Java callers
 * are compiled against and Joran looks up by reflection (to call the setters
 * of components configured in XML): class modifiers, supertypes, and every
 * public/protected constructor, method and field with its exact types,
 * modifiers and declared exceptions.
 *
 * A failure here means that API changed, possibly in a binary- or
 * source-incompatible way, even if all the behavioral tests pass.
 */
@RunWith(RobolectricTestRunner::class)
class JavaApiSignaturesTest {

    @Test
    fun `BasicLogcatConfigurator keeps its Java API`() {
        assertApi(
            BasicLogcatConfigurator::class.java,
            // a Kotlin object since #388: the class couldn't be extended before either
            "public final class",
            "public static final ch.qos.logback.classic.android.BasicLogcatConfigurator INSTANCE",
            "public static void configure(ch.qos.logback.classic.LoggerContext)",
            "public static void configureDefaultContext()",
        )
    }

    @Test
    fun `LogcatAppender keeps its Java API`() {
        assertApi(
            LogcatAppender::class.java,
            "public class extends ch.qos.logback.core.UnsynchronizedAppenderBase<$EVENT>",
            "public LogcatAppender()",
            "public void start()",
            "public void append($EVENT)",
            "public $ENCODER getEncoder()",
            "public void setEncoder($ENCODER)",
            "public $ENCODER getTagEncoder()",
            "public void setTagEncoder($ENCODER)",
            "public boolean getCheckLoggable()",
            "public void setCheckLoggable(boolean)",
            "protected java.lang.String getTag($EVENT)",
        )
    }

    @Test
    fun `LogcatAppender encoders default to PatternLayoutEncoder in XML config`() {
        // Joran instantiates this class for an <encoder> or <tagEncoder> element without a class attribute
        for (setter in listOf("setEncoder", "setTagEncoder")) {
            val method = LogcatAppender::class.java.getMethod(setter, LayoutWrappingEncoder::class.java)
            val defaultClass = method.getAnnotation(DefaultClass::class.java)?.value?.java

            assertEquals(PatternLayoutEncoder::class.java, defaultClass, setter)
        }
    }

    @Test
    fun `SQLiteAppender keeps its Java API`() {
        assertApi(
            SQLiteAppender::class.java,
            "public class extends ch.qos.logback.core.UnsynchronizedAppenderBase<$EVENT>",
            "public SQLiteAppender()",
            "public void start()",
            "public void stop()",
            "public void append($EVENT)",
            // finalize() (which closed the database) was dropped in #388; call stop()
            "public ch.qos.logback.classic.db.names.DBNameResolver getDbNameResolver()",
            "public void setDbNameResolver(ch.qos.logback.classic.db.names.DBNameResolver)",
            "public java.lang.String getFilename()",
            "public void setFilename(java.lang.String)",
            "public java.lang.String getMaxHistory()",
            "public void setMaxHistory(java.lang.String)",
            "public long getMaxHistoryMs()",
            "public java.io.File getDatabaseFile(java.lang.String)",
            "public ch.qos.logback.classic.android.SQLiteLogCleaner getLogCleaner()",
            "public void setLogCleaner(ch.qos.logback.classic.android.SQLiteLogCleaner)",
            // package-private test seam, not public API
            ignore = setOf("setClock"),
        )
    }

    @Test
    fun `SQLiteLogCleaner keeps its Java API`() {
        assertApi(
            SQLiteLogCleaner::class.java,
            "public abstract interface",
            "public abstract void performLogCleanup(" +
                "android.database.sqlite.SQLiteDatabase, ch.qos.logback.core.util.Duration)",
        )
    }

    @Test
    fun `Clock and SystemClock keep their shape`() {
        // package-private types: their visibility is not part of the API
        assertApi(
            Class.forName("ch.qos.logback.classic.android.Clock"),
            "abstract interface",
            "abstract long currentTimeMillis()",
            pinVisibility = false,
        )
        assertApi(
            Class.forName("ch.qos.logback.classic.android.SystemClock"),
            "final class implements ch.qos.logback.classic.android.Clock",
            "SystemClock()",
            "long currentTimeMillis()",
            pinVisibility = false,
        )
    }

    @Test
    fun `AndroidContextUtil keeps its Java API`() {
        assertApi(
            AndroidContextUtil::class.java,
            "public class",
            "public AndroidContextUtil()",
            "public AndroidContextUtil(android.content.Context)",
            "public static boolean containsProperties(java.lang.String)",
            "public static void setApplicationContext(android.content.Context)",
            // public since #388 (was protected), pairing with setApplicationContext()
            "public static android.content.Context getContext()",
            "public static final ch.qos.logback.core.android.AndroidContextUtil\$Companion Companion",
            "public void setupProperties(ch.qos.logback.core.Context)",
            "public void createAppExternalStorageDirs()",
            "public java.lang.String getMountedExternalStorageDirectoryPath()",
            "public java.lang.String getExternalStorageDirectoryPath()",
            "public java.lang.String getExternalFilesDirectoryPath()",
            "public java.lang.String getCacheDirectoryPath()",
            "public java.lang.String getExternalCacheDirectoryPath()",
            "public java.lang.String getPackageName()",
            "public java.lang.String getFilesDirectoryPath()",
            "public java.lang.String getNoBackupFilesDirectoryPath()",
            "public java.lang.String getDatabaseDirectoryPath()",
            "public java.lang.String getDatabasePath(java.lang.String)",
            "public java.lang.String getVersionCode()",
            "public java.lang.String getVersionName()",
        )
    }

    @Test
    fun `SystemPropertiesProxy keeps its Java API`() {
        assertApi(
            SystemPropertiesProxy::class.java,
            // final since #388: with only a private constructor it couldn't be extended before either
            "public final class",
            "public static final ch.qos.logback.core.android.SystemPropertiesProxy\$Companion Companion",
            "public static ch.qos.logback.core.android.SystemPropertiesProxy getInstance()",
            "public void setClassLoader(java.lang.ClassLoader) throws java.lang.ClassNotFoundException, " +
                "java.lang.SecurityException, java.lang.NoSuchMethodException",
            "public java.lang.String get(java.lang.String, java.lang.String) throws java.lang.IllegalArgumentException",
            "public java.lang.Boolean getBoolean(java.lang.String, boolean) throws java.lang.IllegalArgumentException",
        )
    }

    /**
     * Asserts that [type] has exactly the given [declaration] (modifiers and
     * supertypes) and API [members]: its constructors, methods and fields that
     * are public or protected (with [pinVisibility] off: not private), except
     * those named in [ignore]. Compiler-generated (synthetic or bridge) members
     * are not API.
     */
    private fun assertApi(
        type: Class<*>,
        declaration: String,
        vararg members: String,
        ignore: Set<String> = emptySet(),
        pinVisibility: Boolean = true,
    ) {
        val renderer = ApiRenderer(pinVisibility)
        val expected = listOf(declaration) + members.sorted()
        val actual = listOf(renderer.declaration(type)) + renderer.members(type, ignore).sorted()

        assertEquals(
            expected.joinToString("\n"),
            actual.joinToString("\n"),
            "API of ${type.name} changed\n  missing: ${expected - actual}\n  unexpected: ${actual - expected}\n",
        )
    }

    /** Renders an API like Java declarations, with fully qualified (generic) types. */
    private class ApiRenderer(private val pinVisibility: Boolean) {
        private val visibility = if (pinVisibility) Modifier.PUBLIC or Modifier.PROTECTED else 0

        fun declaration(type: Class<*>): String = buildString {
            val mask = visibility or Modifier.FINAL or Modifier.ABSTRACT or Modifier.INTERFACE
            append(Modifier.toString(type.modifiers and mask))
            if (!type.isInterface) append(" class")
            type.genericSuperclass?.takeIf { it != Any::class.java }?.let { append(" extends ${it.typeName}") }
            if (type.genericInterfaces.isNotEmpty()) {
                append(if (type.isInterface) " extends " else " implements ")
                append(type.genericInterfaces.render())
            }
        }.trim()

        fun members(type: Class<*>, ignore: Set<String>): List<String> {
            val members: List<Member> = type.declaredConstructors.toList() +
                type.declaredMethods.filterNot { it.isBridge } + type.declaredFields
            return members.filter { isApi(it) && it.name !in ignore }.map(::render)
        }

        private fun isApi(member: Member): Boolean = !member.isSynthetic && if (pinVisibility) {
            member.modifiers and (Modifier.PUBLIC or Modifier.PROTECTED) != 0
        } else {
            !Modifier.isPrivate(member.modifiers)
        }

        private fun render(member: Member): String {
            val declaration = when (member) {
                is Constructor<*> -> member.declaringClass.simpleName +
                    "(${member.genericParameterTypes.render()})${member.genericExceptionTypes.throwsClause()}"
                is Method -> "${member.genericReturnType.typeName} ${member.name}" +
                    "(${member.genericParameterTypes.render()})${member.genericExceptionTypes.throwsClause()}"
                is Field -> "${member.genericType.typeName} ${member.name}"
                else -> error("unexpected member: $member")
            }
            return listOf(modifiers(member), declaration).filter { it.isNotEmpty() }.joinToString(" ")
        }

        private fun modifiers(member: Member): String {
            // a final method can't be overridden, which doesn't apply to static
            // methods or to the methods of a final class (pinned by its declaration)
            val isFinalRelevant = member !is Method ||
                !(Modifier.isStatic(member.modifiers) || Modifier.isFinal(member.declaringClass.modifiers))
            val mask = visibility or Modifier.STATIC or Modifier.ABSTRACT or
                if (isFinalRelevant) Modifier.FINAL else 0
            return Modifier.toString(member.modifiers and mask)
        }

        private fun Array<out Type>.render() = joinToString { it.typeName }

        private fun Array<out Type>.throwsClause() = if (isEmpty()) "" else " throws ${render()}"
    }

    private companion object {
        const val EVENT = "ch.qos.logback.classic.spi.ILoggingEvent"
        const val ENCODER = "ch.qos.logback.core.encoder.LayoutWrappingEncoder<$EVENT>"
    }
}
