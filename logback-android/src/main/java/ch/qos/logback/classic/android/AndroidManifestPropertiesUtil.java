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
package ch.qos.logback.classic.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import ch.qos.logback.classic.android.ASaxEventRecorder;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.CommonPathUtil;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.Loader;

/**
 * Utility for adding context properties to an InterpretationContext
 * based on manifest attributes from AndroidManifest.xml
 *
 * @author Anthony Trinh
 */
public class AndroidManifestPropertiesUtil {

  /**
   * Sets context properties specific to this application. This reads the
   * application's AndroidManifest.xml for its manifest attributes. If the
   * context already contains properties for the manifest attributes, then
   * nothing is done.
   *
   * @param context context to modify
   * @throws JoranException an error occurred while reading AndroidManifest.xml
   */
  public static void setAndroidProperties(Context context) throws JoranException {

    // filter for dummy element to ignore everything...we're just after the manifest attributes
    ASaxEventRecorder rec = new ASaxEventRecorder();
    rec.setFilter("-");
    rec.setAttributeWatch("manifest");

    StatusManager sm = context.getStatusManager();

    InputStream stream = Loader.getClassLoaderOfObject(context).getResourceAsStream("AndroidManifest.xml");
    if (stream == null) {
      sm.add(new WarnStatus("Could not find AndroidManifest.xml", context));
      return;
    }

    try {
      rec.recordEvents(stream);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }

    // set property for SD card path (only if mounted)
    context.putProperty(CoreConstants.EXT_DIR_KEY, CommonPathUtil.getMountedExternalStorageDirectoryPath());

    // set properties for specific manifest attributes from AndroidManifest.xml
    Map<String,String> manifestAttrs = rec.getAttributeWatchValues();
    for (String key : manifestAttrs.keySet()) {
      if (key.equals("android:versionName")) {
        context.putProperty(CoreConstants.VERSION_NAME_KEY, manifestAttrs.get(key));
      } else if (key.equals("android:versionCode")) {
        context.putProperty(CoreConstants.VERSION_CODE_KEY, manifestAttrs.get(key));
      } else if (key.equals("package")) {
        context.putProperty(CoreConstants.PACKAGE_NAME_KEY, manifestAttrs.get(key));
      }
    }

    // set data directory based on package name from manifest
    String packageName = manifestAttrs.get("package");
    if (packageName != null && packageName.length() > 0) {
      context.putProperty(CoreConstants.DATA_DIR_KEY, CommonPathUtil.getFilesDirectoryPath(packageName));
    } else {
      sm.add(new WarnStatus("Package name not found. Some properties cannot be set.", context));
    }
  }
}
