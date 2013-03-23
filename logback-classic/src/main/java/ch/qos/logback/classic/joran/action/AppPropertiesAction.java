/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.joran.action;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;

import ch.qos.logback.classic.android.ASaxEventRecorder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.CommonPathUtil;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.Loader;

/**
 * Action that sets application properties in the context
 *
 * @author Anthony Trinh
 */
public class AppPropertiesAction extends Action {

  /**
   * Sets context properties specific to this application. This reads the
   * application's AndroidManifest.xml for its package name.
   *
   * @param ic interpretation context
   * @throws ActionException an error occurred while reading AndroidManifest.xml
   */
  private void setAndroidProperties(InterpretationContext ic) throws ActionException {

    // filter for dummy element to ignore everything...we're just after the package attribute
    ASaxEventRecorder rec = new ASaxEventRecorder();
    rec.setFilter("-");
    rec.setAttributeWatch("manifest", "package");

    StatusManager sm = context.getStatusManager();

    InputStream stream = Loader.getClassLoaderOfObject(this).getResourceAsStream("AndroidManifest.xml");
    if (stream == null) {
      sm.add(new WarnStatus("Could not find AndroidManifest.xml", context));
      return;
    }

    try {
      rec.recordEvents(stream);
    } catch (JoranException e) {
      throw new ActionException(e);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }

    // set property for SD card path (only if mounted)
    context.putProperty(CoreConstants.EXT_DIR_KEY, CommonPathUtil.getMountedExternalStorageDirectoryPath());

    // set properties for package name and the app's data directory
    String packageName = rec.getAttributeWatchValue();
    if (packageName != null && packageName.length() > 0) {
      context.putProperty(CoreConstants.PACKAGE_KEY, packageName);
      context.putProperty(CoreConstants.DATA_DIR_KEY, CommonPathUtil.getFilesDirectoryPath(packageName));
    } else {
      sm.add(new WarnStatus("Package name not found. Some properties cannot be set.", context));
    }
  }

  @Override
  public void begin(InterpretationContext ic, String name, Attributes attributes)
      throws ActionException {
    setAndroidProperties(ic);
  }

  @Override
  public void end(InterpretationContext ic, String name) throws ActionException {
    // nothing to do
  }
}
