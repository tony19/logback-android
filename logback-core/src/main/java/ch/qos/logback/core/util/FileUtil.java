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
package ch.qos.logback.core.util;

import java.io.File;

public class FileUtil {

  static public boolean isParentDirectoryCreationRequired(File file) {
    File parent = file.getParentFile();
    return (parent != null && !parent.exists());
  }

  static public boolean createMissingParentDirectories(File file) {
    File parent = file.getParentFile();
    if (parent == null) {
      throw new IllegalStateException(file + " should not have a null parent");
    }
    if (parent.exists()) {
      throw new IllegalStateException(file + " should not have existing parent directory");
    }
    return parent.mkdirs();
  }

  /**
   * Prepends a string to a path if the path is relative. If the path
   * is already absolute, the same path is returned (nothing changed).
   * This is useful for converting relative paths to absolute ones,
   * given the absolute directory path as a prefix.
   *
   * @param prefix string to prepend to the evaluated path if it's not
   * already absolute
   * @param path path to evaluate
   * @return path (prefixed if relative)
   */
  public static String prefixRelativePath(String prefix, String path) {
    if (prefix != null && !prefix.trim().isEmpty() && !new File(path).isAbsolute()) {
      path = prefix + "/" + path;
    }
    return path;
  }
}
