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
package ch.qos.logback.core.testUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ch.qos.logback.core.util.CoreTestConstants;

public class EnvUtilForTests {

  static public boolean isWindows() {
    return System.getProperty("os.name").indexOf("Windows") != -1;
  }

  static public boolean isMac() {
    return System.getProperty("os.name").indexOf("Mac") != -1;
  }

  static public boolean isLinux() {
    return System.getProperty("os.name").indexOf("Linux") != -1;
  }


  static public boolean isRunningOnSlowJenkins() {
    return System.getProperty(CoreTestConstants.SLOW_JENKINS) != null;
  }


  static public String getLocalHostName() {
    InetAddress localhostIA;
    try {
      localhostIA = InetAddress.getLocalHost();
      return localhostIA.getHostName();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  static public boolean isLocalHostNameInList(String[] hostList) {
    String localHostName = getLocalHostName();
    if (localHostName == null) {
      return false;
    }
    for (String host : hostList) {
      if (host.equalsIgnoreCase(localHostName)) {
        return true;
      }
    }
    return false;
  }


  public static String getPathToBash() {
    if (EnvUtilForTests.isLinux()) {
      return CoreTestConstants.BASH_PATH_ON_LINUX;
    }
    if (EnvUtilForTests.isLocalHostNameInList(new String[]{"hetz", "het"})) {
      return CoreTestConstants.BASH_PATH_ON_CYGWIN;
    }
    return null;
  }
}
