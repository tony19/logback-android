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
package ch.qos.logback.core.util;

import java.lang.reflect.Constructor;
import java.util.Properties;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.SystemPropertiesProxy;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.subst.NodeToStringTransformer;

/**
 * @author Ceki Gulcu
 */
public class OptionHelper {

  public static Object instantiateByClassName(String className,
                                              Class<?> superClass, Context context) throws IncompatibleClassException,
          DynamicClassLoadingException {
    ClassLoader classLoader = Loader.getClassLoaderOfObject(context);
    return instantiateByClassName(className, superClass, classLoader);
  }

  public static Object instantiateByClassName(String className,
                                              Class<?> superClass, ClassLoader classLoader)
          throws IncompatibleClassException, DynamicClassLoadingException {
    return instantiateByClassNameAndParameter(className, superClass, classLoader, null, null);
  }

  public static Object instantiateByClassNameAndParameter(String className,
                                                          Class<?> superClass, ClassLoader classLoader, Class<?> type, Object parameter)
          throws IncompatibleClassException, DynamicClassLoadingException {

    if (className == null) {
      throw new NullPointerException();
    }
    try {
      Class<?> classObj = null;
      classObj = classLoader.loadClass(className);
      if (!superClass.isAssignableFrom(classObj)) {
        throw new IncompatibleClassException(superClass, classObj);
      }
      if (type == null) {
        return classObj.getConstructor().newInstance();
      } else {
        Constructor<?> constructor = classObj.getConstructor(type);
        return constructor.newInstance(parameter);
      }
    } catch (IncompatibleClassException ice) {
      throw ice;
    } catch (Throwable t) {
      throw new DynamicClassLoadingException("Failed to instantiate type "
              + className, t);
    }
  }

  final static String DELIM_DEFAULT = ":-";
  final static int DELIM_DEFAULT_LEN = 2;
  final static String _IS_UNDEFINED = "_IS_UNDEFINED";

  /**
   * @see #substVars(String, PropertyContainer, PropertyContainer)
   * @param val string to be evaluated
   * @param pc1 property container
   * @return the substituted string
   */
  public static String substVars(String val, PropertyContainer pc1) {
    return substVars(val, pc1, null);
  }

  /**
   * See  http://logback.qos.ch/manual/configuration.html#variableSubstitution
   * @param input string to be evaluated
   * @param pc0 primary property container
   * @param pc1 secondary property container
   * @return the substituted string
   */
  public static String substVars(String input, PropertyContainer pc0, PropertyContainer pc1) {
    try {
      return NodeToStringTransformer.substituteVariable(input, pc0, pc1);
    } catch (ScanException e) {
      throw new IllegalArgumentException("Failed to parse input [" + input + "]", e);
    }
  }

  /**
   * Very similar to <code>System.getProperty</code> except that the
   * {@link SecurityException} is absorbed.
   *
   * @param key The key to search for.
   * @param def The default value to return.
   * @return the string value of the system property, or the default value if
   *         there is no property with that key.
   */
  public static String getSystemProperty(String key, String def) {
    try {
      return System.getProperty(key, def);
    } catch (SecurityException e) {
      return def;
    }
  }

  /**
   * Lookup a key from the environment.
   *
   * @param key the environment variable's key
   * @return value corresponding to key from the OS environment
   */
  public static String getEnv(String key) {
    try {
      return System.getenv(key);
    } catch (SecurityException e) {
      return null;
    }
  }

  /**
   * Gets an Android system property
   *
   * @param key The key to search for
   * @return the string value of the system property
   */
  public static String getAndroidSystemProperty(String key) {
    try {
      return SystemPropertiesProxy.getInstance().get(key, null);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Very similar to <code>System.getProperty</code> except that the
   * {@link SecurityException} is absorbed. Also checks Android
   * system properties as a fallback.
   *
   * @param key The key to search for.
   * @return the string value of the system property.
   */
  public static String getSystemProperty(String key) {
    try {
      String prop = System.getProperty(key);
      return (prop == null) ? getAndroidSystemProperty(key) : prop;
    } catch (SecurityException e) {
      return null;
    }
  }

  public static void setSystemProperties(ContextAware contextAware, Properties props) {
    for (Object o : props.keySet()) {
      String key = (String) o;
      String value = props.getProperty(key);
      setSystemProperty(contextAware, key, value);
    }
  }

  public static void setSystemProperty(ContextAware contextAware, String key, String value) {
    try {
      System.setProperty(key, value);
    } catch (SecurityException e) {
      contextAware.addError("Failed to set system property [" + key + "]", e);
    }
  }

  /**
   * Very similar to {@link System#getProperties()} except that the
   * {@link SecurityException} is absorbed.
   *
   * @return the system properties
   */
  public static Properties getSystemProperties() {
    try {
      return System.getProperties();
    } catch (SecurityException e) {
      return new Properties();
    }
  }

  /**
   * Return a String[] of size two. The first item containing the key part and the second item
   * containing a default value specified by the user. The second item will be null if no default value
   * is specified.
   *
   * @param key
   * @return array, where item 0 is the key, and item 1 is the default value
   */
  static public String[] extractDefaultReplacement(String key) {
    String[] result = new String[2];
    if(key == null)
      return result;

    result[0] = key;
    int d = key.indexOf(DELIM_DEFAULT);
    if (d != -1) {
      result[0] = key.substring(0, d);
      result[1] = key.substring(d + DELIM_DEFAULT_LEN);
    }
    return result;
  }

  /**
   * If <code>value</code> is "true", then <code>true</code> is returned. If
   * <code>value</code> is "false", then <code>true</code> is returned.
   * Otherwise, <code>default</code> is returned.
   * <p>
   * Case of value is unimportant.
   * @param value string to be evaluated
   * @param defaultValue default value if value could not be converted
   * @return the equivalent boolean value
   */
  public static boolean toBoolean(String value, boolean defaultValue) {
    if (value == null) {
      return defaultValue;
    }

    String trimmedVal = value.trim();

    if ("true".equalsIgnoreCase(trimmedVal)) {
      return true;
    }

    if ("false".equalsIgnoreCase(trimmedVal)) {
      return false;
    }

    return defaultValue;
  }

  public static boolean isEmpty(String str) {
    return ((str == null) || CoreConstants.EMPTY_STRING.equals(str));
  }


}
