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

import ch.qos.logback.core.util.EnvUtil;
import ch.qos.logback.core.util.OptionHelper;
import android.os.Environment;

/**
 * This class provides utility methods to get common paths
 * on the Android filessytem.
 *
 * @author Anthony Trinh
 * @since 1.0.8-1
 */
public abstract class CommonPathUtil {
  private static final String ASSETS_DIRECTORY = "assets";

  /**
   * Gets the path to the external storage directory only if
   * mounted.
   *
   * @return the absolute path to the external storage directory;
   * or {@code null} if not mounted.
   */
  public static String getMountedExternalStorageDirectoryPath() {
    if (EnvUtil.isAndroidOS()) {
      String path = null;
      String state = Environment.getExternalStorageState();
      if (state.equals(Environment.MEDIA_MOUNTED) ||
          state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
        path = Environment.getExternalStorageDirectory().getAbsolutePath();
      }
      return path;
    } else {
      return "/mnt/sdcard";
    }
  }

  /**
   * Gets the path to the external storage directory
   *
   * @return the absolute path to the external storage directory
   */
  public static String getExternalStorageDirectoryPath() {
    if (EnvUtil.isAndroidOS()) {
      return Environment.getExternalStorageDirectory().getAbsolutePath();
    } else {
      String extDir = OptionHelper.getEnv("EXTERNAL_STORAGE");
      return (extDir == null) ? "/sdcard" : extDir;
    }
  }

  /**
   * Returns the absolute path to the directory on the Android
   * filesystem where files are stored for the current application.
   * Unlike the equivalent function in Android, this function does
   * not create the directory if it's non-existent.
   *
   * @param packageName name of the application package
   * @return the absolute path to the files directory
   * (example: "/data/data/com.example/files")
   */
  public static String getFilesDirectoryPath(String packageName) {
    String dataDir = EnvUtil.isAndroidOS() ?
            Environment.getDataDirectory().getAbsolutePath() : "/data";
    return dataDir + "/data/" + packageName + "/files";
  }

  /**
   * Gets the relative path to the assets directory within the jar
   *
   * @return the relative path to the assets directory within the jar
   */
  public static String getAssetsDirectoryPath() {
    return ASSETS_DIRECTORY;
  }

  /**
   * Returns the absolute path to the directory on the Android
   * filesystem where databases are stored for the current application.
   *
   * @param packageName name of the application package
   * @return the absolute path to the databases directory
   * (example: "/data/data/com.example/databases")
   */
  public static String getDatabaseDirectoryPath(String packageName) {
    String dataDir = EnvUtil.isAndroidOS() ?
            Environment.getDataDirectory().getAbsolutePath() : "/data";
    return dataDir + "/data/" + packageName + "/databases";
  }

}
