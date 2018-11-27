/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.android;

import android.annotation.TargetApi;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.CoreConstants;

/**
 * This class provides utility methods to get common directory paths and
 * context data (such as package name).
 *
 * @author Anthony Trinh
 * @since 1.0.8-1
 */
public class AndroidContextUtil {
  private ContextWrapper context;

  public AndroidContextUtil() {
    this(getContext());
  }

  public AndroidContextUtil(ContextWrapper contextWrapper) {
    this.context = contextWrapper;
  }

  /**
   * Sets properties for use in configs
   * @param context logger context whose property map is updated
   */
  public void setupProperties(LoggerContext context) {
    // legacy properties
    Properties props = new Properties();
    props.setProperty(CoreConstants.DATA_DIR_KEY, getFilesDirectoryPath());
    final String extDir = getMountedExternalStorageDirectoryPath();
    if (extDir != null) {
      props.setProperty(CoreConstants.EXT_DIR_KEY, extDir);
    }
    props.setProperty(CoreConstants.PACKAGE_NAME_KEY, getPackageName());
    props.setProperty(CoreConstants.VERSION_CODE_KEY, getVersionCode());
    props.setProperty(CoreConstants.VERSION_NAME_KEY, getVersionName());

    context.putProperties(props);
  }

  private static ContextWrapper getContext() {
    try {
      Class<?> c = Class.forName("android.app.AppGlobals");
      Method method = c.getDeclaredMethod("getInitialApplication");
      return (ContextWrapper)method.invoke(c);
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
    if (state.equals(Environment.MEDIA_MOUNTED) ||
        state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
      path = absPath(Environment.getExternalStorageDirectory());
    }
    return path;
  }

  /**
   * Gets the path to the external storage directory
   *
   * @return the absolute path to the external storage directory
   */
  public String getExternalStorageDirectoryPath() {
    return Environment.getExternalStorageDirectory().getAbsolutePath();
  }

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
  @TargetApi(21)
  public String getNoBackupFilesDirectoryPath() {
    return Build.VERSION.SDK_INT >= 21 &&
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
    return this.context != null
            && this.context.getDatabasePath("x") != null
            ? this.context.getDatabasePath("x").getParent()
            : "";
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
        PackageInfo pkgInfo = this.context.getPackageManager().getPackageInfo(getPackageName(), 0);
        versionCode = "" + pkgInfo.versionCode;
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
    return versionCode;
  }

  public String getVersionName() {
    String versionName = "";
    if (this.context != null) {
      try {
        PackageInfo pkgInfo = this.context.getPackageManager().getPackageInfo(getPackageName(), 0);
        versionName = pkgInfo.versionName;
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
    return versionName != null ? versionName : "";
  }

  private String absPath(File file) {
    return file != null ? file.getAbsolutePath() : "";
  }
}
