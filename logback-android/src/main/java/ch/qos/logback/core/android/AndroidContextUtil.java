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
package ch.qos.logback.core.android;

import android.annotation.TargetApi;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;

/**
 * This class provides utility methods to get common directory paths and
 * context data (such as package name).
 *
 * @author Anthony Trinh
 * @since 1.0.8-1
 */
public class AndroidContextUtil {
  private static final AtomicReference<android.content.Context> CONTEXT_HOLDER = new AtomicReference<>();

  private final android.content.Context context;

  public AndroidContextUtil() {
    this(getContext());
  }

  public AndroidContextUtil(android.content.Context context) {
    this.context = (context == null) ? null : context.getApplicationContext();
  }

  public static boolean containsProperties(String value) {
    return value.contains(CoreConstants.DATA_DIR_KEY)
            || value.contains(CoreConstants.EXT_DIR_KEY)
            || value.contains(CoreConstants.PACKAGE_NAME_KEY)
            || value.contains(CoreConstants.VERSION_CODE_KEY)
            || value.contains(CoreConstants.VERSION_NAME_KEY);
  }

  /**
   * Sets properties for use in configs
   * @param context logger context whose property map is updated
   */
  public void setupProperties(Context context) {
    // legacy properties
    context.putProperty(CoreConstants.DATA_DIR_KEY, getFilesDirectoryPath());
    final String extDir = getMountedExternalStorageDirectoryPath();
    if (extDir != null) {
      context.putProperty(CoreConstants.EXT_DIR_KEY, extDir);
    }
    context.putProperty(CoreConstants.PACKAGE_NAME_KEY, getPackageName());
    context.putProperty(CoreConstants.VERSION_CODE_KEY, getVersionCode());
    context.putProperty(CoreConstants.VERSION_NAME_KEY, getVersionName());
  }

  public static void setApplicationContext(android.content.Context context) {
    CONTEXT_HOLDER.set((context == null) ? null : context.getApplicationContext());
  }

  protected static android.content.Context getContext() {
    android.content.Context context = CONTEXT_HOLDER.get();
    if (context != null) {
        return context.getApplicationContext();
    }

    try {
      Class<?> c = Class.forName("android.app.AppGlobals");
      Method method = c.getDeclaredMethod("getInitialApplication");
      context = (android.content.Context) method.invoke(c);
      return (context == null) ? null : context.getApplicationContext();
    } catch (ClassNotFoundException e) {
      //e.printStackTrace();
    } catch (NoSuchMethodException e) {
      //e.printStackTrace();
    } catch (IllegalAccessException e) {
      //e.printStackTrace();
    } catch (InvocationTargetException e) {
      //e.printStackTrace();
    }
    return null;
  }

  /**
   * Gets the path to the external storage directory only if
   * mounted.
   *
   * @return the absolute path to the external storage directory;
   * or {@code null} if not mounted.
   */
  public String getMountedExternalStorageDirectoryPath() {
    String path = null;
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state) ||
        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      path = getExternalStorageDirectoryPath();
    }
    return path;
  }

  /**
   * Gets the path to the external storage directory
   *
   * This API is available on SDK 8+. On API versions 29 onwards,
   * this function uses the implementation in
   * {@link android.content.Context#getExternalFilesDir(java.lang.String)}
   * which is the proposed replacement for
   * {@link android.os.Environment#getExternalStorageDirectory()}.
   *
   * @return the absolute path to the external storage directory
   */
  @TargetApi(Build.VERSION_CODES.FROYO)
  @SuppressWarnings("deprecation")
  public String getExternalStorageDirectoryPath() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return getExternalFilesDirectoryPath();
    } else {
      return absPath(Environment.getExternalStorageDirectory());
    }
  }

  /**
   * Gets the path to the external storage directory
   *
   * This API is available on SDK 8+. This function uses the implementation in
   * {@link android.content.Context#getExternalFilesDir(java.lang.String)}.
   *
   * @return the absolute path to the external storage directory
   */
  @TargetApi(Build.VERSION_CODES.FROYO)
  public String getExternalFilesDirectoryPath() {
    return this.context != null
            ? absPath(this.context.getExternalFilesDir(null))
            : "";
  }

  public String getCacheDirectoryPath() {
    return this.context != null
            ? absPath(this.context.getCacheDir())
            : "";
  }

  public String getExternalCacheDirectoryPath() {
    return this.context != null
            ? absPath(this.context.getExternalCacheDir())
            : "";
  }

  public String getPackageName() {
    return this.context != null
            ? this.context.getPackageName()
            : "";
  }

  /**
   * Returns the absolute path to the directory on the Android
   * filesystem where files are stored for the current application.
   * Unlike the equivalent function in Android, this function does
   * not create the directory if it's non-existent.
   *
   * @return the absolute path to the files directory
   * (example: "/data/data/com.example/files")
   */
  public String getFilesDirectoryPath() {
    return this.context != null
            ? absPath(this.context.getFilesDir())
            : "";
  }

  /**
   * Returns the absolute path to the directory on the Android
   * filesystem similar to {@link #getFilesDirectoryPath()}.
   * The difference is these files are excluded from automatic
   * backup to remote storage by {@code android.app.backup.BackupAgent}.
   * This API is only available on SDK 21+. On older versions,
   * this function returns an empty string.
   *
   * @return the absolute path to the files directory
   * (example: "/data/data/com.example/nobackup/files")
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public String getNoBackupFilesDirectoryPath() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            this.context != null
            ? absPath(this.context.getNoBackupFilesDir())
            : "";
  }

  /**
   * Returns the absolute path to the directory on the Android
   * filesystem where databases are stored for the current application.
   *
   * @return the absolute path to the databases directory
   * (example: "/data/data/com.example/databases")
   */
  public String getDatabaseDirectoryPath() {
    File dbPath = (this.context == null) ? null : this.context.getDatabasePath("x");
    return (dbPath != null) ? dbPath.getParent() : "";
  }

  public String getDatabasePath(String databaseName) {
    return this.context != null
            ? absPath(this.context.getDatabasePath(databaseName))
            : "";
  }

  public String getVersionCode() {
    String versionCode = "";
    if (this.context != null) {
      try {
        PackageManager pm = this.context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageInfo(getPackageName(), 0);
        versionCode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                ? Long.toString(pkgInfo.getLongVersionCode())
                : Integer.toString(pkgInfo.versionCode)
                ;
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
    return versionCode;
  }

  public String getVersionName() {
    String versionName = "";
    if (this.context != null) {
      try {
        PackageManager pm = this.context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageInfo(getPackageName(), 0);
        versionName = pkgInfo.versionName;
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
    return versionName != null ? versionName : "";
  }

  private static String absPath(File file) {
    return file != null ? file.getAbsolutePath() : "";
  }
}
