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
package ch.qos.logback.classic.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.ASaxEventRecorder;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.CommonPathUtil;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;

// contributors
// Ted Graham, Matt Fowles, see also http://jira.qos.ch/browse/LBCORE-32

/**
 * This class contains logback's logic for automatic configuration
 *
 * @author Ceki Gulcu
 */
public class ContextInitializer {

  final public static String  AUTOCONFIG_FILE        = "logback.xml";
  final public static String  TEST_AUTOCONFIG_FILE   = "logback-test.xml";
  final public static String  CONFIG_FILE_PROPERTY   = "logback.configurationFile";
  final public static String  STATUS_LISTENER_CLASS  = "logback.statusListenerClass";
  final public static String  SYSOUT                 = "SYSOUT";
  final private static String TAG_MANIFEST           = "manifest";
  final private static String TAG_LOGBACK            = "logback";
  final private static String ATTR_PACKAGE_NAME      = "package";
  final private static String MANIFEST_FILE          = "AndroidManifest.xml";
  final private static String ASSETS_DIR             = CommonPathUtil.getAssetsDirectoryPath();
  final private static String SDCARD_DIR = CommonPathUtil.getExternalStorageDirectoryPath() + "/logback";

  final LoggerContext loggerContext;

  public ContextInitializer(LoggerContext loggerContext) {
    this.loggerContext = loggerContext;
  }

  /**
   * Gets an input stream to the application's AndroidManifest.xml
   */
  private InputStream openManifest(ClassLoader classLoader, boolean updateStatus) {
    // fetch the URL to AndroidManifest.xml
    URL url = getResource(MANIFEST_FILE, classLoader, updateStatus);
    if (url == null) {
      return null;
    }
    // open the file (via URL connection's input stream)
    InputStream stream = null;
    try {
      URLConnection conn = url.openConnection();

      // per http://jira.qos.ch/browse/LBCORE-105
      // per http://jira.qos.ch/browse/LBCORE-127
      conn.setUseCaches(false);
      stream = conn.getInputStream();
    } catch (IOException e) {
      StatusManager sm = loggerContext.getStatusManager();
      sm.add(new ErrorStatus("Could not open URL [" + url + "].", e));
    }
    return stream;
  }

  /**
   * Parses the logback SAX events and the app's package name
   * from the manifest
   *
   * @param eventList the list to populate with the SAX events
   * @param packageName the string buffer to populate with the package name
   * @return {@code true} if manifest was parsed; {@code false} otherwise
   */
  private boolean parseManifest(List<SaxEvent> eventList, StringBuffer packageName) {
    // don't try to parse the manifest if not on Android (the classloader
    // will end up grabbing the manifest from the android SDK jar itself)
    if (!CommonPathUtil.isAndroidOS()) {
      return false;
    }

    ClassLoader classLoader = Loader.getClassLoaderOfObject(this);
    InputStream stream = openManifest(classLoader, true);
    if (stream == null) {
      // error already reported in openManifest(), so no need to repeat
      return false;
    }

    // use Android XML resource parser to process AndroidManifest.xml
    // (which is in compressed binary form; not text)
    ASaxEventRecorder recorder = new ASaxEventRecorder(loggerContext);
    recorder.setFilter(TAG_MANIFEST, TAG_LOGBACK);

    // Since reading the manifest is relatively expensive, we set a
    // watch point in the recorder so that it will catch the package
    // name while parsing SAX events. This saves us from having to
    // re-parse the manifest just for the package name. Two birds...
    recorder.setAttributeWatch(TAG_MANIFEST, ATTR_PACKAGE_NAME);

    // begin parsing...
    try {
      recorder.recordEvents(stream);
    } catch (JoranException e) {
      StatusManager sm = loggerContext.getStatusManager();
      sm.add(new ErrorStatus("Could not parse AndroidManifest.xml", e));
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }

    // return the event list and the package name
    List<SaxEvent> events = recorder.getSaxEventList();
    if (events != null) {
      eventList.addAll(events);
    }
    packageName.append(recorder.getAttributeWatchValue());

    return true;
  }

  /**
   * Finds a configuration file by system property
   * @return the URL to the file; or {@code null} if not found
   */
  private URL findConfigFileURLFromSystemProperties(boolean updateStatus) {
    String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
    if (logbackConfigFile != null) {
      ClassLoader classLoader = Loader.getClassLoaderOfObject(this);
      URL result = null;
      try {
        result = new URL(logbackConfigFile);
        return result;
      } catch (MalformedURLException e) {
        // so, resource is not a URL:
        // attempt to get the resource from the class path
        result = Loader.getResource(logbackConfigFile, classLoader);
        if (result != null) {
          return result;
        }
        File f = new File(logbackConfigFile);
        if (f.exists() && f.isFile()) {
          try {
            result = f.toURI().toURL();
            return result;
          } catch (MalformedURLException e1) {
          }
        }
      } finally {
        if (updateStatus) {
          statusOnResourceSearch(logbackConfigFile, classLoader, result);
        }
      }
    }
    return null;
  }

  /**
   * Finds a configuration file in the application's assets directory
   * @return the URL to the file; or {@code null} if not found
   */
  private URL findConfigFileURLFromAssets(boolean updateStatus) {
    ClassLoader myClassLoader = Loader.getClassLoaderOfObject(this);
    URL url = getResource(ASSETS_DIR + "/" + TEST_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }
    return getResource(ASSETS_DIR + "/" + AUTOCONFIG_FILE, myClassLoader, updateStatus);
  }

  /**
   * Uses the given classloader to search for a resource
   * @return the URL to the resource; or {@code null} if not found
   */
  private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
    URL url = Loader.getResource(filename, myClassLoader);
    if (updateStatus) {
      statusOnResourceSearch(filename, myClassLoader, url);
    }
    return url;
  }

  /**
   * Finds a configuration file on the SD card
   * @return the configuration file
   */
  private File findConfigFileFromSD(boolean updateStatus, String packageName) {
    List<String> sdSearchPaths =
        new LinkedList<String>(Arrays.asList(
            SDCARD_DIR + "/" + TEST_AUTOCONFIG_FILE,
            SDCARD_DIR + "/" + AUTOCONFIG_FILE
        ));

    if (!OptionHelper.isEmpty(packageName)) {
      // make sure test config is first in list
      sdSearchPaths.add(0, SDCARD_DIR + "/" + packageName + "/" + AUTOCONFIG_FILE);
      sdSearchPaths.add(0, SDCARD_DIR + "/" + packageName + "/" + TEST_AUTOCONFIG_FILE);
    }

    // get first config file found in search path
    File file = null;
    for (String path : sdSearchPaths) {
      File f = new File(path);
      if (!f.exists() && f.isFile()) {
        file = f;
        break;
      }
    }

    if (updateStatus) {
      StatusManager sm = loggerContext.getStatusManager();
      if (file != null) {
        sm.add(new InfoStatus("Found config in SD card: ["+ file.getAbsolutePath() +"]", loggerContext));
      } else {
        sm.add(new InfoStatus("No config in SD card", loggerContext));
      }
    }

    return file;
  }

  /**
   * Configures logback with the first configuration found in the following search path.
   * If not found, configuration defaults to {@link BasicLogcatConfigurator}.
   *
   * <ol>
   *    <li>/sdcard/${PACKAGE}/logback-test.xml</li>
   *    <li>/sdcard/${PACKAGE}/logback.xml</li>
   *    <li>/sdcard/logback-test.xml</li>
   *    <li>/sdcard/logback.xml</li>
   *    <li>jar:file://AndroidManifest.xml</li>
   *    <li>${logback.configurationFile}</li>
   *    <li>jar:file://assets/logback-test.xml</li>
   *    <li>jar:file://assets/logback.xml</li>
   * </ol>
   */
  public void autoConfig() throws JoranException {
    StatusListenerConfigHelper.installIfAsked(loggerContext);

    // parse the manifest early so we can get the package name
    // for the SD-card search
    List<SaxEvent> saxEvents = new ArrayList<SaxEvent>();
    StringBuffer packageNameBuf = new StringBuffer();
    boolean hasManifestInfo = parseManifest(saxEvents, packageNameBuf);
    String packageName = packageNameBuf.toString();

    // set context properties
    if (hasManifestInfo) {
      loggerContext.putProperty(CoreConstants.PACKAGE_KEY, packageName);
      loggerContext.putProperty(CoreConstants.DATA_DIR_KEY, CommonPathUtil.getFilesDirectoryPath(packageName));
    }
    loggerContext.putProperty(CoreConstants.EXT_DIR_KEY, CommonPathUtil.getMountedExternalStorageDirectoryPath());

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);

    // search SD card for config
    boolean verbose = true;
    boolean configured = false;
    File file = findConfigFileFromSD(verbose, packageName);
    if (file != null) {
      configurator.doConfigure(file);
      configured = true;
    }

    // search manifest
    if (!configured && hasManifestInfo) {
      configurator.doConfigure(saxEvents);
      configured = true;
    }

    // search system property
    if (!configured) {
      URL url = findConfigFileURLFromSystemProperties(verbose);
      if (url != null) {
        configurator.doConfigure(url);
        configured = true;
      }
    }

    // search assets
    if (!configured) {
      URL url = findConfigFileURLFromAssets(verbose);
      if (url != null) {
        configurator.doConfigure(url);
        configured = true;
      }
    }

    // fall back to BasicLogcatConfigurator
    if (!configured) {
      BasicLogcatConfigurator.configure(loggerContext);
    }
  }

  /**
   * Adds a status message to flag resources that occur multiple times on classpath
   */
  private void multiplicityWarning(String resourceName, ClassLoader classLoader) {
    Set<URL> urlSet = null;
    StatusManager sm = loggerContext.getStatusManager();
    try {
      urlSet = Loader.getResourceOccurenceCount(resourceName, classLoader);
    } catch (IOException e) {
      sm.add(new ErrorStatus("Failed to get url list for resource [" + resourceName + "]",
              loggerContext, e));
    }
    if (urlSet != null && urlSet.size() > 1) {
      sm.add(new WarnStatus("Resource [" + resourceName + "] occurs multiple times on the classpath.",
              loggerContext));
      for (URL url : urlSet) {
        sm.add(new WarnStatus("Resource [" + resourceName + "] occurs at [" + url.toString() + "]",
                loggerContext));
      }
    }
  }

  /**
   * Adds a status message for the result of the resource search
   */
  private void statusOnResourceSearch(String resourceName, ClassLoader classLoader, URL url) {
    StatusManager sm = loggerContext.getStatusManager();
    if (url == null) {
      sm.add(new InfoStatus("Could NOT find resource [" + resourceName + "]",
              loggerContext));
    } else {
      sm.add(new InfoStatus("Found resource [" + resourceName + "] at [" + url.toString() + "]",
              loggerContext));
      multiplicityWarning(resourceName, classLoader);
    }
  }
}
