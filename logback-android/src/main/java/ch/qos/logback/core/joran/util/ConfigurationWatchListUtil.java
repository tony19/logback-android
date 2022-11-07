/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.joran.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;

import java.net.URL;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class ConfigurationWatchListUtil {

  final static ConfigurationWatchListUtil origin = new ConfigurationWatchListUtil();

  private ConfigurationWatchListUtil() {
  }

  public static void registerConfigurationWatchList(Context context, ConfigurationWatchList cwl) {
    context.putObject(CoreConstants.CONFIGURATION_WATCH_LIST, cwl);
  }

  public static void setMainWatchURL(Context context, URL url) {
    if (context == null) return;

    ConfigurationWatchList cwl = getConfigurationWatchList(context);
    if (cwl == null) {
      cwl = new ConfigurationWatchList();
      cwl.setContext(context);
      context.putObject(CoreConstants.CONFIGURATION_WATCH_LIST, cwl);
    } else {
      cwl.clear();
    }
    //setConfigurationWatchListResetFlag(context, true);
    cwl.setMainURL(url);
  }

  public static URL getMainWatchURL(Context context) {
    ConfigurationWatchList cwl = getConfigurationWatchList(context);
    if (cwl == null) {
      return null;
    } else {
      return cwl.getMainURL();
    }
  }

  public static void addToWatchList(Context context, URL url) {
    ConfigurationWatchList cwl = getConfigurationWatchList(context);
    if (cwl == null) {
      addWarn(context, "Null ConfigurationWatchList. Cannot add " + url);
    } else {
      addInfo(context, "Adding [" + url + "] to configuration watch list.");
      cwl.addToWatchList(url);
    }
  }

//  public static boolean wasConfigurationWatchListReset(Context context) {
//  if (context == null) return false;
//
//    Object o = context.getObject(CoreConstants.CONFIGURATION_WATCH_LIST_RESET);
//    if (o == null)
//      return false;
//    else {
//      return ((Boolean) o).booleanValue();
//    }
//  }
//
//  public static void setConfigurationWatchListResetFlag(Context context, boolean val) {
//    context.putObject(CoreConstants.CONFIGURATION_WATCH_LIST_RESET, Boolean.valueOf(val));
//  }

  public static ConfigurationWatchList getConfigurationWatchList(Context context) {
    if (context == null) return null;
    return (ConfigurationWatchList) context.getObject(CoreConstants.CONFIGURATION_WATCH_LIST);
  }

  static void addStatus(Context context, Status s) {
    if (context == null) {
      System.out.println("Null context in " + ConfigurationWatchList.class.getName());
      return;
    }
    StatusManager sm = context.getStatusManager();
    if (sm == null) return;
    sm.add(s);
  }

  static void addInfo(Context context, String msg) {
    addStatus(context, new InfoStatus(msg, origin));
  }

  static void addWarn(Context context, String msg) {
    addStatus(context, new WarnStatus(msg, origin));
  }
}
