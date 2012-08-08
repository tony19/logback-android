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
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.ASaxEventRecorder;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;
import ch.qos.logback.classic.joran.JoranConfigurator;
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

  final public static String  GROOVY_AUTOCONFIG_FILE = "logback.groovy";
  final public static String  AUTOCONFIG_FILE        = "logback.xml";
  final public static String  TEST_AUTOCONFIG_FILE   = "logback-test.xml";
  final public static String  CONFIG_FILE_PROPERTY   = "logback.configurationFile";
  final public static String  STATUS_LISTENER_CLASS  = "logback.statusListenerClass";
  final public static String  SYSOUT                 = "SYSOUT";
  final private static String TAG_MANIFEST           = "manifest";
  final private static String TAG_LOGBACK            = "logback";
  final private static String MANIFEST_FILE          = "AndroidManifest.xml";
  final private static String ASSETS_DIR             = "/assets/";
  final private static String SDCARD_DIR             = "/sdcard/logback/";
  
  final LoggerContext loggerContext;

  public ContextInitializer(LoggerContext loggerContext) {
    this.loggerContext = loggerContext;
  }
  
  private InputStream openManifest(ClassLoader classLoader, boolean updateStatus) {
    StatusManager sm = loggerContext.getStatusManager();

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
      sm.add(new ErrorStatus("Could not open URL [" + url + "].", e));
    }
    return stream;
  }
  
  /**
   * Configures Logback by reading the configuration from the
   * AndroidManifest.xml
   * 
   * @return {@code true} if successfully processed config from manifest;
   *         {@code false} otherwise
   */
  public boolean configureByManifest() {

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

    boolean ok = false;
    try {
      // begin parsing...
      recorder.recordEvents(stream);

      try {
        stream.close();
      } catch (IOException e) {
      }

      // ...and get the results to pass to Joran
      List<SaxEvent> events = recorder.getSaxEventList();
      if ((events != null) && (events.size() > 0)) {
        JoranConfigurator joran = new JoranConfigurator();
        joran.setContext(loggerContext);
        joran.doConfigure(events);
        ok = true;
      }
    } catch (JoranException e) {
      StatusManager sm = loggerContext.getStatusManager();
      sm.add(new ErrorStatus("Could not configure by AndroidManifest.xml", e));
    }
    return ok;
  }
  
  public void configureByResource(URL url) throws JoranException {
    if (url == null) {
      throw new IllegalArgumentException("URL argument cannot be null");
    }
    if (url.toString().endsWith("groovy")) {
      StatusManager sm = loggerContext.getStatusManager();
      sm.add(new ErrorStatus("Groovy classes are not available on the class path. ABORTING INITIALIZATION.",
              loggerContext));
    }
    if (url.toString().endsWith("xml")) {
    	joranConfigureByResource(url);
    }
  }

  void joranConfigureByResource(URL url) throws JoranException {
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);
    configurator.doConfigure(url);
  }

  private URL findConfigFileURLFromSystemProperties(ClassLoader classLoader, boolean updateStatus) {
    String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
    if (logbackConfigFile != null) {
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

  public URL findURLOfDefaultConfigurationFile(boolean updateStatus, String dir) {
    ClassLoader myClassLoader = Loader.getClassLoaderOfObject(this);
    URL url = findConfigFileURLFromSystemProperties(myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    url = getResource(dir + TEST_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    return getResource(dir + AUTOCONFIG_FILE, myClassLoader, updateStatus);
  }
  
  private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
    URL url = Loader.getResource(filename, myClassLoader);
    if (updateStatus) {
      statusOnResourceSearch(filename, myClassLoader, url);
    }
    return url;
  }

  private File findSDConfigFile(boolean updateStatus) {
    
    File file = new File(SDCARD_DIR + TEST_AUTOCONFIG_FILE);
    if (!file.exists()) {
      file = new File(SDCARD_DIR + AUTOCONFIG_FILE);
    }
    
    if (updateStatus) {
      StatusManager sm = loggerContext.getStatusManager();
      if (file.exists()) {
        sm.add(new InfoStatus("Found config in SD card: ["+ file.getAbsolutePath() +"]", loggerContext));
      } else {
        sm.add(new WarnStatus("No config in SD card", loggerContext));
      }
    }
    
    return file.exists() ? file : null;
  }
  
  public void autoConfig() throws JoranException {
    StatusListenerConfigHelper.installIfAsked(loggerContext);
    
    /* Search for configuration from:
     *   1. SD card
     *   2. Android Manifest
     *   3. assets directory
     *   
     * If not found, fall back to simple LogcatAppender.
     */
    File file = findSDConfigFile(true);
    if (file != null) {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(loggerContext);
      configurator.doConfigure(file);
      
    } else if (!configureByManifest()) {
    	
      URL url = findURLOfDefaultConfigurationFile(true, ASSETS_DIR);
      if (url != null) {
        configureByResource(url);
        
      } else {	
      	BasicLogcatConfigurator.configure(loggerContext);
      }
    }
  }

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
