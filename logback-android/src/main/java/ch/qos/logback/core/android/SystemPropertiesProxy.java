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
package ch.qos.logback.core.android;

import java.lang.reflect.Method;

/**
 * A proxy to get Android's global system properties (as opposed
 * to the default process-level system properties). Settings from
 * `adb setprop` can be accessed from this class.
 */
public class SystemPropertiesProxy {

  private static final SystemPropertiesProxy SINGLETON = new SystemPropertiesProxy(null);
  private Class<?> SystemProperties;
  private Method getString, getBoolean;

  private SystemPropertiesProxy(ClassLoader cl) {
    try {
      setClassLoader(cl);
    } catch (Exception e) {
    }
  }

  /**
   * Gets the singleton instance for this class
   *
   * @return the singleton
   */
  public static SystemPropertiesProxy getInstance() {
    return SINGLETON;
  }

  /**
   * Sets the classloader to lookup the class for android.os.SystemProperties
   *
   * @param cl desired classloader
   * @throws ClassNotFoundException android.os.SystemProperties class not found
   * @throws SecurityException security manager does not allow class loading
   * @throws NoSuchMethodException get/getBoolean method does not exist
   */
  public void setClassLoader(ClassLoader cl)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException {
    if (cl == null) cl = this.getClass().getClassLoader();
    SystemProperties = cl.loadClass("android.os.SystemProperties");
    getString = SystemProperties.getMethod("get", new Class[]{ String.class, String.class });
    getBoolean = SystemProperties.getMethod("getBoolean", new Class[]{ String.class, boolean.class });
  }

  /**
   * Get the value for the given key in the Android system properties
   *
   * @param key
   *          the key to lookup
   * @param def
   *          a default value to return
   * @return an empty string if the key isn't found
   * @throws IllegalArgumentException
   *           if the key exceeds 32 characters
   */
  public String get(String key, String def)
      throws IllegalArgumentException {

    if (SystemProperties == null || getString == null) return null;

    String ret = null;
    try {
      ret = (String) getString.invoke(SystemProperties, new Object[]{ key, def });
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
    }
    // if return value is null or empty, use the default
    // since neither of those are valid values
    if (ret == null || ret.length() == 0) {
      ret = def;
    }
    return ret;
  }

  /**
   * Get the value for the given key in the Android system properties, returned
   * as a boolean.
   *
   * Values 'n', 'no', '0', 'false' or 'off' are considered false. Values 'y',
   * 'yes', '1', 'true' or 'on' are considered true. (case insensitive). If the
   * key does not exist, or has any other value, then the default result is
   * returned.
   *
   * @param key
   *          the key to lookup
   * @param def
   *          a default value to return
   * @return the key parsed as a boolean, or def if the key isn't found or is
   *         not able to be parsed as a boolean.
   * @throws IllegalArgumentException
   *           if the key exceeds 32 characters
   */
  public Boolean getBoolean(String key, boolean def)
      throws IllegalArgumentException {

    if (SystemProperties == null || getBoolean == null) return def;

    Boolean ret = def;
    try {
      ret = (Boolean) getBoolean.invoke(SystemProperties, new Object[]{ key, def });
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
    }
    return ret;
  }
}
