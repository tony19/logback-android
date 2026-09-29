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

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.CoreConstants
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Ownership
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.MethodCall
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.util.ReflectionHelpers
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Hidden framework class that AndroidContextUtil falls back to via reflection */
private const val APP_GLOBALS = "android.app.AppGlobals"

/** Version code 42 with a version code major of 1 (only [PackageInfo.getLongVersionCode] sees the major) */
private const val LONG_VERSION_CODE = (1L shl 32) or 42L

/**
 * Tests the [AndroidContextUtil] class
 *
 * @author Anthony Trinh
 */
@RunWith(RobolectricTestRunner::class)
class AndroidContextUtilTest {
    private lateinit var app: Application
    private lateinit var contextUtil: AndroidContextUtil

    @Before
    fun before() {
        ShadowEnvironment.reset()
        // Ensure no context leaked in from a prior test's setApplicationContext() call
        AndroidContextUtil.setApplicationContext(null)
        app = RuntimeEnvironment.getApplication()
        contextUtil = AndroidContextUtil()
    }

    @After
    fun after() {
        // Reset the static holder so it doesn't leak into other tests
        AndroidContextUtil.setApplicationContext(null)
    }

    // --- context resolution -------------------------------------------------

    @Test
    fun `setApplicationContext is used instead of reflection workaround`() {
        val provided = StandaloneContext(app, "com.example.provided")
        AndroidContextUtil.setApplicationContext(provided)
        assertSame(provided, AndroidContextUtil.getContext())
    }

    @Test
    fun `setApplicationContext is callable from Java as a static method`() {
        // Java code (e.g. an Application's onCreate) calls the static bridge, not the companion
        val provided = StandaloneContext(app, "com.example.provided")
        AndroidContextUtil::class.java.getMethod("setApplicationContext", Context::class.java).invoke(null, provided)
        assertSame(provided, AndroidContextUtil.getContext())
    }

    @Test
    fun `setApplicationContext retains the application context of the given context`() {
        val appContext = StandaloneContext(app, "com.example.provided")
        val component = ComponentContext(appContext)

        AndroidContextUtil.setApplicationContext(component)
        repeat(2) { assertSame(appContext, AndroidContextUtil.getContext()) }

        // the component itself isn't retained (it could leak), so it's asked only once
        assertEquals(1, component.applicationContextRequests)
    }

    @Test
    fun `no-arg constructor picks up provided context`() {
        AndroidContextUtil.setApplicationContext(StandaloneContext(app, "com.example.provided"))
        // The no-arg constructor resolves its context through getContext(), so it
        // should observe the externally provided context.
        assertEquals("com.example.provided", AndroidContextUtil().packageName)
    }

    @Test
    fun `setApplicationContext null falls back to reflection workaround`() {
        AndroidContextUtil.setApplicationContext(StandaloneContext(app, "com.example.provided"))
        AndroidContextUtil.setApplicationContext(null)
        assertSame(app, AndroidContextUtil.getContext())
    }

    @Test
    fun `constructor uses the application context of the given context`() {
        val util = AndroidContextUtil(ComponentContext(StandaloneContext(app, "com.example.provided")))
        assertEquals("com.example.provided", util.packageName)
    }

    @Test
    @Config(shadows = [ShadowAppGlobalsWithoutApplication::class])
    fun `getContext is null when there is no initial application`() {
        assertNull(AndroidContextUtil.getContext())
        assertEquals("", AndroidContextUtil().packageName)
    }

    @Test
    @Config(shadows = [ShadowFailingAppGlobals::class])
    fun `getContext is null when looking up the initial application fails`() {
        assertNull(AndroidContextUtil.getContext())
    }

    @Test
    fun `getContext is null when AppGlobals does not exist`() {
        assertNull(getContextWithAppGlobals(null))
    }

    @Test
    fun `getContext is null when AppGlobals has no getInitialApplication method`() {
        val appGlobals = ByteBuddy().subclass(Any::class.java).name(APP_GLOBALS).make().bytes
        assertNull(getContextWithAppGlobals(appGlobals))
    }

    @Test
    fun `getContext is null when getInitialApplication is inaccessible`() {
        // control: the same AppGlobals with a public getInitialApplication() works
        assertSame(app, getContextWithAppGlobals(appGlobalsReturningApplication(Visibility.PUBLIC)))

        assertNull(getContextWithAppGlobals(appGlobalsReturningApplication(Visibility.PRIVATE)))
    }

    // --- external storage ---------------------------------------------------

    @Test
    fun `getMountedExternalStorageDirectoryPath returns path when mounted`() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        assertEquals(externalFilesDir().absolutePath, contextUtil.mountedExternalStorageDirectoryPath)
    }

    @Test
    fun `getMountedExternalStorageDirectoryPath returns path when mounted read-only`() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED_READ_ONLY)
        assertEquals(externalFilesDir().absolutePath, contextUtil.mountedExternalStorageDirectoryPath)
    }

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when removed`() =
        assertNotMountedWhen(Environment.MEDIA_REMOVED)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when bad removal`() =
        assertNotMountedWhen(Environment.MEDIA_BAD_REMOVAL)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when checking`() =
        assertNotMountedWhen(Environment.MEDIA_CHECKING)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when ejecting`() =
        assertNotMountedWhen(Environment.MEDIA_EJECTING)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when no fs`() =
        assertNotMountedWhen(Environment.MEDIA_NOFS)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when unknown`() =
        assertNotMountedWhen(Environment.MEDIA_UNKNOWN)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when unmountable`() =
        assertNotMountedWhen(Environment.MEDIA_UNMOUNTABLE)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when shared`() =
        assertNotMountedWhen(Environment.MEDIA_SHARED)

    @Test
    fun `getMountedExternalStorageDirectoryPath returns null when unmounted`() =
        assertNotMountedWhen(Environment.MEDIA_UNMOUNTED)

    // issue #315
    @Test
    @Config(shadows = [ShadowEnvironmentWithoutStorageVolume::class])
    fun `getMountedExternalStorageDirectoryPath returns null when the storage state is unavailable`() {
        assertFailsWith<ArrayIndexOutOfBoundsException> { Environment.getExternalStorageState() }
        assertNull(contextUtil.mountedExternalStorageDirectoryPath)
    }

    @Test
    fun `getExternalStorageDirectoryPath is the external files dir on Q and later`() {
        val path = withSdkInt(Build.VERSION_CODES.Q) { contextUtil.externalStorageDirectoryPath }
        assertEquals(externalFilesDir().absolutePath, path)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `getExternalStorageDirectoryPath is the shared storage root before Q`() {
        val path = withSdkInt(Build.VERSION_CODES.P) { contextUtil.externalStorageDirectoryPath }
        assertEquals(Environment.getExternalStorageDirectory().absolutePath, path)
        assertNotEquals(externalFilesDir().absolutePath, path)
    }

    // issue #228
    @Test
    fun `createAppExternalStorageDirs creates missing dirs`() {
        val dirs = deleteAppExternalStorageDirs()

        contextUtil.createAppExternalStorageDirs()

        dirs.forEach { assertTrue(it.isDirectory, "$it") }
    }

    // issue #228
    @Test
    fun `createAppExternalStorageDirs is noop without context`() {
        val dirs = deleteAppExternalStorageDirs()

        AndroidContextUtil(null).createAppExternalStorageDirs() // must not throw

        dirs.forEach { assertFalse(it.exists(), "$it") }
    }

    // issue #431
    @Test
    fun `createAppExternalStorageDirs tolerates broken external storage`() {
        var requests = 0
        val util = AndroidContextUtil(object : StandaloneContext(app) {
            override fun getExternalFilesDir(type: String?): File? {
                requests++
                throw IllegalStateException("Failed to resolve external storage volume")
            }
        })

        util.createAppExternalStorageDirs() // must not throw

        assertEquals(1, requests)
    }

    // --- directory paths ----------------------------------------------------

    @Test
    fun `getFilesDirectoryPath is the app files dir`() {
        assertEquals(app.filesDir.absolutePath, contextUtil.filesDirectoryPath)
        assertTrue(contextUtil.filesDirectoryPath.endsWith("/files"))
    }

    @Test
    fun `getExternalFilesDirectoryPath is the app external files dir`() {
        assertEquals(externalFilesDir().absolutePath, contextUtil.externalFilesDirectoryPath)
    }

    @Test
    fun `getNoBackupFilesDirectoryPath is the app no-backup dir`() {
        assertEquals(app.noBackupFilesDir.absolutePath, contextUtil.noBackupFilesDirectoryPath)
        assertTrue(contextUtil.noBackupFilesDirectoryPath.endsWith("/no_backup"))
    }

    @Test
    fun `getNoBackupFilesDirectoryPath is the app no-backup dir as of Lollipop`() {
        val path = withSdkInt(Build.VERSION_CODES.LOLLIPOP) { contextUtil.noBackupFilesDirectoryPath }
        assertEquals(app.noBackupFilesDir.absolutePath, path)
    }

    @Test
    fun `getNoBackupFilesDirectoryPath is empty before Lollipop`() {
        val path = withSdkInt(Build.VERSION_CODES.KITKAT_WATCH) { contextUtil.noBackupFilesDirectoryPath }
        assertEquals("", path)
    }

    @Test
    fun `getCacheDirectoryPath is the app cache dir`() {
        assertEquals(app.cacheDir.absolutePath, contextUtil.cacheDirectoryPath)
        assertTrue(contextUtil.cacheDirectoryPath.endsWith("/cache"))
    }

    @Test
    fun `getExternalCacheDirectoryPath is the app external cache dir`() {
        assertEquals(externalCacheDir().absolutePath, contextUtil.externalCacheDirectoryPath)
    }

    @Test
    fun `external dir paths are empty when shared storage is unavailable`() {
        val util = AndroidContextUtil(WithoutSharedStorage(app))
        assertEquals("", util.externalFilesDirectoryPath)
        assertEquals("", util.externalCacheDirectoryPath)
    }

    @Test
    fun `getDatabaseDirectoryPath is the parent of the app databases`() {
        assertEquals(app.getDatabasePath("app.db").parent, contextUtil.databaseDirectoryPath)
        assertTrue(contextUtil.databaseDirectoryPath.endsWith("/databases"))
    }

    @Test
    fun `getDatabasePath is the path of the named app database`() {
        assertEquals(app.getDatabasePath("app.db").absolutePath, contextUtil.getDatabasePath("app.db"))
    }

    @Test
    fun `paths are empty without context`() {
        val util = AndroidContextUtil(null)
        assertEquals("", util.filesDirectoryPath)
        assertEquals("", util.externalFilesDirectoryPath)
        assertEquals("", util.externalStorageDirectoryPath)
        assertEquals("", util.noBackupFilesDirectoryPath)
        assertEquals("", util.cacheDirectoryPath)
        assertEquals("", util.externalCacheDirectoryPath)
        assertEquals("", util.databaseDirectoryPath)
        assertEquals("", util.getDatabasePath("app.db"))
    }

    // --- package info -------------------------------------------------------

    @Test
    fun `getPackageName is the app package name`() {
        assertEquals(app.packageName, contextUtil.packageName)
    }

    @Test
    fun `getVersionCode is the long version code on Pie and later`() {
        installedPackageInfo().longVersionCode = LONG_VERSION_CODE
        val versionCode = withSdkInt(Build.VERSION_CODES.P) { contextUtil.versionCode }
        assertEquals(LONG_VERSION_CODE.toString(), versionCode)
    }

    @Test
    fun `getVersionCode is the int version code before Pie`() {
        installedPackageInfo().longVersionCode = LONG_VERSION_CODE
        val versionCode = withSdkInt(Build.VERSION_CODES.O_MR1) { contextUtil.versionCode }
        assertEquals("42", versionCode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `version code and name come from the package manager on Tiramisu and later`() {
        // API 33+ looks the package up with PackageManager.PackageInfoFlags
        installedPackageInfo().longVersionCode = LONG_VERSION_CODE
        installedPackageInfo().versionName = "1.2.3"

        assertEquals(LONG_VERSION_CODE.toString(), contextUtil.versionCode)
        assertEquals("1.2.3", contextUtil.versionName)
    }

    @Test
    fun `getVersionName is the app version name`() {
        installedPackageInfo().versionName = "1.2.3"
        assertEquals("1.2.3", contextUtil.versionName)
    }

    @Test
    fun `getVersionName is empty when the app has no version name`() {
        installedPackageInfo().versionName = null
        assertEquals("", contextUtil.versionName)
    }

    @Test
    fun `version code and name are empty when the package is not installed`() {
        val util = AndroidContextUtil(StandaloneContext(app, "com.example.not.installed"))
        assertEquals("", util.versionCode)
        assertEquals("", util.versionName)
    }

    @Test
    fun `package info is empty without context`() {
        val util = AndroidContextUtil(null)
        assertEquals("", util.packageName)
        assertEquals("", util.versionCode)
        assertEquals("", util.versionName)
    }

    // --- setupProperties ----------------------------------------------------

    @Test
    fun setupProperties() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        installedPackageInfo().apply {
            longVersionCode = LONG_VERSION_CODE
            versionName = "1.2.3"
        }
        val loggerContext = LoggerContext()

        assertNull(loggerContext.getProperty(CoreConstants.DATA_DIR_KEY))
        assertNull(loggerContext.getProperty(CoreConstants.EXT_DIR_KEY))
        assertNull(loggerContext.getProperty(CoreConstants.VERSION_CODE_KEY))
        assertNull(loggerContext.getProperty(CoreConstants.VERSION_NAME_KEY))
        assertNull(loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY))

        contextUtil.setupProperties(loggerContext)

        assertEquals(app.filesDir.absolutePath, loggerContext.getProperty(CoreConstants.DATA_DIR_KEY))
        // the external storage dir is the external files dir on Q and later
        assertEquals(externalFilesDir().absolutePath, loggerContext.getProperty(CoreConstants.EXT_DIR_KEY))
        assertEquals(LONG_VERSION_CODE.toString(), loggerContext.getProperty(CoreConstants.VERSION_CODE_KEY))
        assertEquals("1.2.3", loggerContext.getProperty(CoreConstants.VERSION_NAME_KEY))
        assertEquals(app.packageName, loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY))
    }

    // issue #181
    @Test
    fun `setupProperties includes external dirs`() {
        val loggerContext = LoggerContext()

        assertNull(loggerContext.getProperty(CoreConstants.EXT_FILES_DIR_KEY))
        assertNull(loggerContext.getProperty(CoreConstants.EXT_CACHE_DIR_KEY))

        contextUtil.setupProperties(loggerContext)

        assertEquals(externalFilesDir().absolutePath, loggerContext.getProperty(CoreConstants.EXT_FILES_DIR_KEY))
        assertEquals(externalCacheDir().absolutePath, loggerContext.getProperty(CoreConstants.EXT_CACHE_DIR_KEY))
    }

    @Test
    fun `setupProperties omits EXT_DIR when external storage is not mounted`() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED)
        val loggerContext = LoggerContext()

        contextUtil.setupProperties(loggerContext)

        assertNoProperty(loggerContext, CoreConstants.EXT_DIR_KEY)
    }

    @Test
    fun `setupProperties omits external dirs when shared storage is unavailable`() {
        val loggerContext = LoggerContext()

        AndroidContextUtil(WithoutSharedStorage(app)).setupProperties(loggerContext)

        assertNoProperty(loggerContext, CoreConstants.EXT_FILES_DIR_KEY)
        assertNoProperty(loggerContext, CoreConstants.EXT_CACHE_DIR_KEY)
        assertEquals(app.filesDir.absolutePath, loggerContext.getProperty(CoreConstants.DATA_DIR_KEY))
    }

    @Test
    fun `setupProperties omits external dirs whose paths are null`() {
        // a subclass may report an unavailable dir as null rather than empty
        val util = spy(AndroidContextUtil(app))
        doReturn(null).whenever(util).externalFilesDirectoryPath
        doReturn(null).whenever(util).externalCacheDirectoryPath
        val loggerContext = LoggerContext()

        util.setupProperties(loggerContext)

        assertNoProperty(loggerContext, CoreConstants.EXT_FILES_DIR_KEY)
        assertNoProperty(loggerContext, CoreConstants.EXT_CACHE_DIR_KEY)
        assertEquals(app.filesDir.absolutePath, loggerContext.getProperty(CoreConstants.DATA_DIR_KEY))
    }

    @Test
    fun `setupProperties without context sets empty values`() {
        // EXT_DIR depends on the storage state rather than on the context
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED)
        val loggerContext = LoggerContext()

        AndroidContextUtil(null).setupProperties(loggerContext)

        assertEquals("", loggerContext.getProperty(CoreConstants.DATA_DIR_KEY))
        assertNoProperty(loggerContext, CoreConstants.EXT_DIR_KEY)
        assertNoProperty(loggerContext, CoreConstants.EXT_FILES_DIR_KEY)
        assertNoProperty(loggerContext, CoreConstants.EXT_CACHE_DIR_KEY)
        assertEquals("", loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY))
        assertEquals("", loggerContext.getProperty(CoreConstants.VERSION_CODE_KEY))
        assertEquals("", loggerContext.getProperty(CoreConstants.VERSION_NAME_KEY))
    }

    // --- containsProperties -------------------------------------------------

    // issue #181
    @Test
    fun `containsProperties detects external dir keys`() {
        assertTrue(AndroidContextUtil.containsProperties("\${EXT_FILES_DIR}/logs/app.log"))
        assertTrue(AndroidContextUtil.containsProperties("\${EXT_CACHE_DIR}/logs/app.log"))
        assertFalse(AndroidContextUtil.containsProperties("/absolute/path/app.log"))
    }

    @Test
    fun `containsProperties detects each Android property`() {
        val keys = listOf(
            CoreConstants.DATA_DIR_KEY,
            CoreConstants.EXT_DIR_KEY,
            CoreConstants.EXT_FILES_DIR_KEY,
            CoreConstants.EXT_CACHE_DIR_KEY,
            CoreConstants.PACKAGE_NAME_KEY,
            CoreConstants.VERSION_CODE_KEY,
            CoreConstants.VERSION_NAME_KEY,
        )
        for (key in keys) {
            assertTrue(AndroidContextUtil.containsProperties("\${$key}/app.log"), key)
        }
    }

    @Test
    fun `containsProperties ignores other properties`() {
        assertFalse(AndroidContextUtil.containsProperties("\${LOG_DIR}/\${HOSTNAME}.log"))
    }

    // --- helpers ------------------------------------------------------------

    private fun externalFilesDir(): File = checkNotNull(app.getExternalFilesDir(null))

    private fun externalCacheDir(): File = checkNotNull(app.externalCacheDir)

    /** Deletes the app-specific external storage dirs (issue #228), returning them */
    private fun deleteAppExternalStorageDirs(): List<File> =
        listOf(externalFilesDir(), externalCacheDir()).onEach { assertTrue(it.delete(), "$it") }

    private fun installedPackageInfo(): PackageInfo =
        checkNotNull(shadowOf(app.packageManager).getInternalMutablePackageInfo(app.packageName))

    /** Asserts that [key] isn't set at all (not even to `null`) */
    private fun assertNoProperty(loggerContext: LoggerContext, key: String) =
        assertFalse(key in loggerContext.copyOfPropertyMap, "unexpected property $key")

    private fun assertNotMountedWhen(state: String) {
        ShadowEnvironment.setExternalStorageState(state)
        assertNull(contextUtil.mountedExternalStorageDirectoryPath)
    }

    private fun <T> withSdkInt(sdkInt: Int, block: () -> T): T {
        val original = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdkInt)
        return try {
            block()
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", original)
        }
    }

    /** An `AppGlobals` class whose static `getInitialApplication()` has the given visibility */
    private fun appGlobalsReturningApplication(visibility: Visibility): ByteArray =
        ByteBuddy().subclass(Any::class.java).name(APP_GLOBALS)
            .defineMethod("getInitialApplication", Application::class.java, visibility, Ownership.STATIC)
            .intercept(MethodCall.invoke(RuntimeEnvironment::class.java.getMethod("getApplication")))
            .make().bytes

    /**
     * Calls `getContext()` on a fresh copy of [AndroidContextUtil] whose
     * `android.app.AppGlobals` is the class defined by [appGlobals], or
     * doesn't exist if that's `null`
     */
    private fun getContextWithAppGlobals(appGlobals: ByteArray?): Any? {
        val loader = AppGlobalsReplacingClassLoader(checkNotNull(javaClass.classLoader), appGlobals)
        val util = loader.loadClass(AndroidContextUtil::class.java.name)
        assertNotEquals<Class<*>>(AndroidContextUtil::class.java, util)
        return util.getDeclaredMethod("getContext").apply { isAccessible = true }.invoke(null)
    }
}

/**
 * A context that is its own application context (like an [Application]), so
 * [AndroidContextUtil] uses it as is, reporting the given package name
 */
private open class StandaloneContext(
    base: Context,
    private val reportedPackageName: String = base.packageName,
) : ContextWrapper(base) {
    override fun getApplicationContext(): Context = this
    override fun getPackageName(): String = reportedPackageName
}

/**
 * A component's context (like an Activity's) whose application context is
 * [application]. It reports a package name of its own, so that any use of it in
 * place of its application context is detectable.
 */
private class ComponentContext(private val application: Context) : ContextWrapper(application) {
    var applicationContextRequests = 0
        private set

    override fun getApplicationContext(): Context {
        applicationContextRequests++
        return application
    }

    override fun getPackageName(): String = "com.example.component"
}

/** An app whose shared storage is unavailable (e.g., not mounted) */
private class WithoutSharedStorage(base: Context) : StandaloneContext(base) {
    override fun getExternalFilesDir(type: String?): File? = null
    override fun getExternalCacheDir(): File? = null
}

/**
 * Child-first class loader that re-defines [AndroidContextUtil] (and its nested
 * classes) from the class path, so that its reflective lookup of
 * `android.app.AppGlobals` resolves through this loader: to the class defined by
 * [appGlobals], or to none (`ClassNotFoundException`) if that's `null`
 */
private class AppGlobalsReplacingClassLoader(
    private val delegate: ClassLoader,
    private val appGlobals: ByteArray?,
) : ClassLoader(delegate) {
    private val utilClassName = AndroidContextUtil::class.java.name

    override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
        findLoadedClass(name) ?: when {
            name == APP_GLOBALS -> define(name, appGlobals ?: throw ClassNotFoundException(name))
            name == utilClassName || name.startsWith("$utilClassName\$") -> define(name, bytesOf(name))
            else -> super.loadClass(name, resolve)
        }
    }

    private fun bytesOf(className: String): ByteArray {
        val resource = className.replace('.', '/') + ".class"
        return checkNotNull(delegate.getResourceAsStream(resource)) { resource }.use { it.readBytes() }
    }

    private fun define(name: String, bytes: ByteArray): Class<*> = defineClass(name, bytes, 0, bytes.size)
}

/** `AppGlobals` in a process that has no application (yet) */
@Implements(className = APP_GLOBALS)
private class ShadowAppGlobalsWithoutApplication {
    companion object {
        @JvmStatic
        @Implementation
        fun getInitialApplication(): Application? = null
    }
}

/** `AppGlobals` whose lookup of the initial application fails */
@Implements(className = APP_GLOBALS)
private class ShadowFailingAppGlobals {
    companion object {
        @JvmStatic
        @Implementation
        fun getInitialApplication(): Application = throw IllegalStateException("no activity thread")
    }
}

/**
 * `Environment` of a process without an external storage volume, e.g. a
 * shell-context tool started via app_process (issue #315)
 */
@Implements(Environment::class)
private class ShadowEnvironmentWithoutStorageVolume {
    companion object {
        @JvmStatic
        @Implementation
        fun getExternalStorageState(): String = throw ArrayIndexOutOfBoundsException("length=0; index=0")
    }
}
