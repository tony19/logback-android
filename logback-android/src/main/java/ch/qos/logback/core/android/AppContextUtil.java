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

import android.content.ContextWrapper;
import android.os.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class provides utility methods to get common directory paths and
 * context data (such as package name).
 *
 * @author Anthony Trinh
 * @since 1.0.8-1
 */
public class AndroidContextUtil {
  private static final String ASSETS_DIRECTORY = "assets";
  private ContextWrapper context;

  public AndroidContextUtil() {
    this.context = getContext();
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
      path = Environment.getExternalStorageDirectory().getAbsolutePath();
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
            ? this.context.getExternalFilesDir(null).getAbsolutePath()
            : "";
  }

  public String getCacheDirectoryPath() {
    return this.context != null
            ? this.context.getCacheDir().getAbsolutePath()
            : "";
  }

  public String getExternalCacheDirectoryPath() {
    return this.context != null
            ? this.context.getExternalCacheDir().getAbsolutePath()
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
            ? this.context.getFilesDir().getAbsolutePath()
            : "";
  }

  /**
   * Gets the relative path to the assets directory within the jar
   *
   * @return the relative path to the assets directory within the jar
   */
  public String getAssetsDirectoryPath() {
    return ASSETS_DIRECTORY;
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
            ? this.context.getDatabasePath("x").getParent()
            : "";
  }

}
