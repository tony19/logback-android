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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class EnvUtil {

  private static final Pattern versionPattern = Pattern.compile("^(1\\.)?([0-9]+)");

  private EnvUtil() {}

  /**
   * Heuristically determines whether the current OS is Android
   */
  static public boolean isAndroidOS() {
    String osname = OptionHelper.getSystemProperty("os.name");
    String root = OptionHelper.getEnv("ANDROID_ROOT");
    String data = OptionHelper.getEnv("ANDROID_DATA");

    return osname != null && osname.contains("Linux") &&
        root != null && root.contains("/system") &&
        data != null && data.contains("/data");
  }

  static private boolean isJDK_N_OrHigher(int n) {
    Matcher matcher = versionPattern.matcher(System.getProperty("java.version", ""));
    if (matcher.find()) {
      return n <= Integer.parseInt(matcher.group(2));
    }
    return false;
  }

  static public boolean isJDK5() {
    return isJDK_N_OrHigher(5);
  }

  static public boolean isJDK7OrHigher() {
    return isJDK_N_OrHigher(7);
  }
}
