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

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment

import java.io.File
import java.util.concurrent.atomic.AtomicReference

import ch.qos.logback.core.Context
import ch.qos.logback.core.CoreConstants

/**
 * This class provides utility methods to get common directory paths and
 * context data (such as package name).
 *
 * @author Anthony Trinh
 * @since 1.0.8-1
 */
public open class AndroidContextUtil @JvmOverloads constructor(
    context: android.content.Context? = getContext()
) {
    private val context: android.content.Context? = context?.applicationContext

    /**
     * Sets properties for use in configs
     *
     * @param loggerContext logger context whose property map is updated
     */
    public fun setupProperties(loggerContext: Context) {
        // legacy properties
        loggerContext.putProperty(CoreConstants.DATA_DIR_KEY, filesDirectoryPath)
        mountedExternalStorageDirectoryPath?.let { extDir ->
            loggerContext.putProperty(CoreConstants.EXT_DIR_KEY, extDir)
        }
        // Android-version-independent paths to the app-specific external
        // directories, writable without permissions on API 19+ (issue #181)
        externalFilesDirectoryPath.takeIf { it.isNotEmpty() }?.let { extFilesDir ->
            loggerContext.putProperty(CoreConstants.EXT_FILES_DIR_KEY, extFilesDir)
        }
        externalCacheDirectoryPath.takeIf { it.isNotEmpty() }?.let { extCacheDir ->
            loggerContext.putProperty(CoreConstants.EXT_CACHE_DIR_KEY, extCacheDir)
        }
        loggerContext.putProperty(CoreConstants.PACKAGE_NAME_KEY, packageName)
        loggerContext.putProperty(CoreConstants.VERSION_CODE_KEY, versionCode)
        loggerContext.putProperty(CoreConstants.VERSION_NAME_KEY, versionName)
    }

    /**
     * The path to the external storage directory only if mounted;
     * `null` if not mounted.
     */
    public val mountedExternalStorageDirectoryPath: String?
        get() {
            val state = try {
                Environment.getExternalStorageState()
            } catch (e: RuntimeException) {
                // Environment.getExternalStorageState throws
                // ArrayIndexOutOfBoundsException in processes without an external
                // storage volume, e.g. shell-context tools started via app_process
                // (issue #315); treat it the same as "not mounted"
                return null
            }
            return when (state) {
                Environment.MEDIA_MOUNTED,
                Environment.MEDIA_MOUNTED_READ_ONLY -> externalStorageDirectoryPath
                else -> null
            }
        }

    /**
     * The absolute path to the external storage directory.
     *
     * On API 29 onwards, this is the implementation in
     * [android.content.Context.getExternalFilesDir], which is the
     * replacement for the deprecated
     * [android.os.Environment.getExternalStorageDirectory].
     */
    public val externalStorageDirectoryPath: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            externalFilesDirectoryPath
        } else {
            @Suppress("DEPRECATION")
            absPath(Environment.getExternalStorageDirectory())
        }

    /**
     * The absolute path to the external files directory
     * ([android.content.Context.getExternalFilesDir]).
     */
    public val externalFilesDirectoryPath: String
        get() = absPath(context?.getExternalFilesDir(null))

    /**
     * Requests creation of the app-specific external storage directories
     * (`Android/data/<package>/files` and `Android/data/<package>/cache`).
     *
     * Under scoped storage (Android 11+), these base directories can only be
     * created by the platform: a plain [File.mkdirs] fails until the app
     * requests them via [android.content.Context.getExternalFilesDir] or
     * [android.content.Context.getExternalCacheDir], both of which create the
     * directory if it doesn't already exist (issue #228).
     *
     * This is a no-op if no Android context is available, and it tolerates
     * broken external-storage states (issue #431).
     */
    public open fun createAppExternalStorageDirs() {
        val context = this.context ?: return
        try {
            context.getExternalFilesDir(null)
            context.externalCacheDir
        } catch (e: RuntimeException) {
            // tolerate broken external-storage state (issue #431); the caller's
            // subsequent directory-creation attempt fails and reports as usual
        }
    }

    /**
     * The absolute path to the application's cache directory.
     */
    public val cacheDirectoryPath: String
        get() = absPath(context?.cacheDir)

    /**
     * The absolute path to the application's external cache directory.
     */
    public val externalCacheDirectoryPath: String
        get() = absPath(context?.externalCacheDir)

    /**
     * The application's package name.
     */
    public val packageName: String
        get() = context?.packageName ?: ""

    /**
     * The absolute path to the directory on the Android filesystem where
     * files are stored for the current application. Unlike the equivalent
     * function in Android, this does not create the directory if it's
     * non-existent.
     *
     * (example: "/data/data/com.example/files")
     */
    public val filesDirectoryPath: String
        get() = absPath(context?.filesDir)

    /**
     * The absolute path to the directory on the Android filesystem similar
     * to [filesDirectoryPath]. The difference is these files are excluded
     * from automatic backup to remote storage by
     * `android.app.backup.BackupAgent`.
     *
     * (example: "/data/data/com.example/nobackup/files")
     */
    public val noBackupFilesDirectoryPath: String
        get() = absPath(context?.noBackupFilesDir)

    /**
     * The absolute path to the directory on the Android filesystem where
     * databases are stored for the current application.
     *
     * (example: "/data/data/com.example/databases")
     */
    public val databaseDirectoryPath: String
        get() = context?.getDatabasePath("x")?.parent ?: ""

    /**
     * Gets the absolute path to the database with the given name
     *
     * @param databaseName name of the target database
     * @return the absolute path to the database
     */
    public fun getDatabasePath(databaseName: String): String =
        absPath(context?.getDatabasePath(databaseName))

    /**
     * The application's version code, as a string.
     */
    public val versionCode: String
        get() {
            val pkgInfo = packageInfo ?: return ""
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toString()
            }
        }

    /**
     * The application's version name.
     */
    public val versionName: String
        get() = packageInfo?.versionName ?: ""

    private val packageInfo: PackageInfo?
        get() = try {
            val packageManager = context?.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager?.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager?.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun absPath(file: File?): String = file?.absolutePath ?: ""

    public companion object {
        private val CONTEXT_HOLDER = AtomicReference<android.content.Context?>()

        @JvmStatic
        public fun containsProperties(value: String): Boolean =
            value.contains(CoreConstants.DATA_DIR_KEY) ||
                value.contains(CoreConstants.EXT_DIR_KEY) ||
                value.contains(CoreConstants.EXT_FILES_DIR_KEY) ||
                value.contains(CoreConstants.EXT_CACHE_DIR_KEY) ||
                value.contains(CoreConstants.PACKAGE_NAME_KEY) ||
                value.contains(CoreConstants.VERSION_CODE_KEY) ||
                value.contains(CoreConstants.VERSION_NAME_KEY)

        /**
         * Provides an Android [android.content.Context] for the framework to use
         * instead of the reflection-based workaround (which may break under Google's
         * restrictions on non-SDK interfaces). The application context is extracted and
         * retained, so any [android.content.ContextWrapper] (Application, Activity,
         * Service) may be passed. This must be called before the context is first needed
         * by the framework (e.g. in the application's `onCreate`).
         *
         * @param context the context to use; pass `null` to clear it
         */
        @JvmStatic
        public fun setApplicationContext(context: android.content.Context?) {
            CONTEXT_HOLDER.set(context?.applicationContext)
        }

        /**
         * Gets the application context provided via [setApplicationContext], falling
         * back to the reflection-based lookup of the initial application.
         *
         * @return the application context; or `null` if unavailable
         */
        @JvmStatic
        public fun getContext(): android.content.Context? {
            CONTEXT_HOLDER.get()?.let { context ->
                return context.applicationContext
            }

            return try {
                val appGlobals = Class.forName("android.app.AppGlobals")
                val method = appGlobals.getDeclaredMethod("getInitialApplication")
                (method.invoke(appGlobals) as? android.content.Context)?.applicationContext
            } catch (e: ReflectiveOperationException) {
                null
            }
        }
    }
}
