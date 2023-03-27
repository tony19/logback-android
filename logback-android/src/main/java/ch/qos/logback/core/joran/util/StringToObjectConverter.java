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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAware;

/**
 * Utility class which can convert string into objects.
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public class StringToObjectConverter {

  private static final Class<?>[] STRING_CLASS_PARAMETER = new Class[] { String.class };

  static public boolean canBeBuiltFromSimpleString(Class<?> parameterClass) {
    Package p = parameterClass.getPackage();
    if (parameterClass.isPrimitive()) {
      return true;
    } else if (p != null && "java.lang".equals(p.getName())) {
      return true;
    } else if (followsTheValueOfConvention(parameterClass)) {
      return true;
    } else if (parameterClass.isEnum()) {
      return true;
    } else if (isOfTypeCharset(parameterClass)) {
      return true;
    }
    return false;
  }

  /**
   * Convert <code>val</code> a String parameter to an object of a given type.
   * @param ca context
   * @param val string representation of value
   * @param type target class to convert val into
   * @return the object created from the string
   */
  @SuppressWarnings("unchecked")
  public static Object convertArg(ContextAware ca, String val, Class<?> type) {
    if (val == null) {
      return null;
    }
    String v = val.trim();
    if (String.class.isAssignableFrom(type)) {
      return v;
    } else if (Integer.TYPE.isAssignableFrom(type)) {
      return Integer.valueOf(v);
    } else if (Long.TYPE.isAssignableFrom(type)) {
      return Long.valueOf(v);
    } else if (Float.TYPE.isAssignableFrom(type)) {
      return Float.valueOf(v);
    } else if (Double.TYPE.isAssignableFrom(type)) {
      return Double.valueOf(v);
    } else if (Boolean.TYPE.isAssignableFrom(type)) {
      if ("true".equalsIgnoreCase(v)) {
        return Boolean.TRUE;
      } else if ("false".equalsIgnoreCase(v)) {
        return Boolean.FALSE;
      }
    } else if (type.isEnum()) {
      return convertToEnum(ca, v, (Class<? extends Enum<?>>) type);
    } else if (StringToObjectConverter.followsTheValueOfConvention(type)) {
      return convertByValueOfMethod(ca, type, v);
    } else if (isOfTypeCharset(type)) {
      return convertToCharset(ca, val);
    }

    return null;
  }

  static private boolean isOfTypeCharset(Class<?> type) {
    return Charset.class.isAssignableFrom(type);
  }

  static private Charset convertToCharset(ContextAware ca, String val) {
    try {
      return Charset.forName(val);
    } catch (UnsupportedCharsetException e) {
      ca.addError("Failed to get charset [" + val + "]", e);
      return null;
    }
  }

  // returned value may be null and in most cases it is null.
  public static Method getValueOfMethod(Class<?> type) {
    try {
      return type.getMethod(CoreConstants.VALUE_OF, STRING_CLASS_PARAMETER);
    } catch (NoSuchMethodException e) {
      return null;
    } catch (SecurityException e) {
      return null;
    }
  }

  static private boolean followsTheValueOfConvention(Class<?> parameterClass) {
    Method valueOfMethod = getValueOfMethod(parameterClass);
    if (valueOfMethod == null) {
      return false;
    }
    int mod = valueOfMethod.getModifiers();
    return Modifier.isStatic(mod);
  }

  private static Object convertByValueOfMethod(ContextAware ca, Class<?> type,
      String val) {
    try {
      Method valueOfMethod = type.getMethod(CoreConstants.VALUE_OF,
              STRING_CLASS_PARAMETER);
      return valueOfMethod.invoke(null, val);
    } catch (Exception e) {
      ca.addError("Failed to invoke " + CoreConstants.VALUE_OF
          + "{} method in class [" + type.getName() + "] with value [" + val
          + "]");
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static Object convertToEnum(ContextAware ca, String val,
      Class<? extends Enum> enumType) {
    return Enum.valueOf(enumType, val);
  }

  boolean isBuildableFromSimpleString() {
    return false;
  }
}
