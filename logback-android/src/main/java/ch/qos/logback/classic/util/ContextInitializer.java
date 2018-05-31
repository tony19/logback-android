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
package ch.qos.logback.classic.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.android.AndroidContextUtil;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusListenerConfigHelper;

// contributors
// Ted Graham, Matt Fowles, see also http://jira.qos.ch/browse/LBCORE-32

/**
 * This class contains logback's logic for automatic configuration
 *
 * @author Anthony Trinh
 * @author Ceki Gulcu
 */
public class ContextInitializer {

  final public static String  AUTOCONFIG_FILE        = "assets/logback.xml";
  final public static String  CONFIG_FILE_PROPERTY   = "logback.configurationFile";

  final ClassLoader classLoader;
  final LoggerContext loggerContext;

  public ContextInitializer(LoggerContext loggerContext) {
    this.loggerContext = loggerContext;
    this.classLoader = Loader.getClassLoaderOfObject(this);
  }

  /**
   * Finds a configuration file by system property
   * @return the file; or {@code null} if not found
   */
  private URL findConfigFileFromSystemProperties(boolean updateStatus) {
    String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
    if (logbackConfigFile != null) {
      URL result = null;
      try {
        File file = new File(logbackConfigFile);
        if (file.exists() && file.isFile()) {
          if (updateStatus) {
            statusOnResourceSearch(logbackConfigFile, this.classLoader, logbackConfigFile);
          }
          result = file.toURI().toURL();
        } else {
          result = new URL(logbackConfigFile);
        }
        return result;
      } catch (MalformedURLException e) {
        // so, resource is not a URL:
        // attempt to get the resource from the class path
        result = Loader.getResource(logbackConfigFile, this.classLoader);
        if (result != null) {
          return result;
        }
      } finally {
        if (updateStatus) {
          statusOnResourceSearch(logbackConfigFile, this.classLoader, result != null ? result.toString() : null);
        }
      }
    }
    return null;
  }

  /**
   * Finds a configuration file in the application's assets directory
   * @return the URL of the file; or {@code null} if not found
   */
  private URL findConfigFileURLFromAssets(boolean updateStatus) {
    return getResource(AUTOCONFIG_FILE, this.classLoader, updateStatus);
  }

  /**
   * Uses the given classloader to search for a resource
   * @return the URL to the resource; or {@code null} if not found
   */
  private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
    URL url = myClassLoader.getResource(filename);
    if (updateStatus) {
      String resourcePath = null;
      if (url != null) {
        resourcePath = filename;
      }
      statusOnResourceSearch(filename, myClassLoader, resourcePath);
    }
    return url;
  }

  /**
   * Configures logback with the first configuration found in the following search path.
   * If no configuration found, nothing is done and logging is disabled.
   *
   * <ol>
   *    <li>${logback.configurationFile} (a system property)</li>
   *    <li>jar:file://assets/logback.xml</li>
   * </ol>
   */
  public void autoConfig() throws JoranException {
    StatusListenerConfigHelper.installIfAsked(loggerContext);

    new AndroidContextUtil().setupProperties(loggerContext);

    boolean verbose = true;
    boolean configured = false;

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);

    // search system property
    if (!configured) {
      URL url = findConfigFileFromSystemProperties(verbose);
      if (url != null) {
        configurator.doConfigure(url);
        configured = true;
      }
    }

    // search assets
    if (!configured) {
      URL assetsConfigUrl = findConfigFileURLFromAssets(verbose);
      if (assetsConfigUrl != null) {
        configurator.doConfigure(assetsConfigUrl);
        configured = true;
      }
    }
  }

  /**
   * Adds a status message for the result of the resource search
   */
  private void statusOnResourceSearch(String resourceName, ClassLoader classLoader, String path) {
    StatusManager sm = loggerContext.getStatusManager();
    if (path == null) {
      sm.add(new InfoStatus("Could NOT find resource [" + resourceName + "]",
              loggerContext));
    } else {
      sm.add(new InfoStatus("Found resource [" + resourceName + "] at [" + path + "]",
              loggerContext));
    }
  }
}
