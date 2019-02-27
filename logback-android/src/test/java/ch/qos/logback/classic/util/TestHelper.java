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
package ch.qos.logback.classic.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestHelper {

  private static final Method ADD_SUPPRESSED_METHOD;

  static {
    Method method = null;
    try {
      method = Throwable.class.getMethod("addSuppressed", Throwable.class);
    } catch (NoSuchMethodException e) {
      // ignore, will get thrown in Java < 7
    }
    ADD_SUPPRESSED_METHOD = method;
  }

  public static boolean suppressedSupported() {
    return ADD_SUPPRESSED_METHOD != null;
  }

  public static void addSuppressed(Throwable outer, Throwable suppressed) throws InvocationTargetException, IllegalAccessException {
    if(suppressedSupported()) {
      ADD_SUPPRESSED_METHOD.invoke(outer, suppressed);
    }
  }

  static public Throwable makeNestedException(int level) {
    if (level == 0) {
      return new Exception("nesting level=" + level);
    }
    Throwable cause = makeNestedException(level - 1);
    return new Exception("nesting level =" + level, cause);
  }

  /**
   * Usage:
   * <pre>
   * String s = "123";
   * positionOf("1").in(s) < positionOf("3").in(s)
   * </pre>
   *
   * @param pattern Plain text to be found
   * @return StringPosition fluent interface
   */
  public static StringPosition positionOf(String pattern) {
    return new StringPosition(pattern);
  }

  public static class StringPosition {
    private final String pattern;

    public StringPosition(String pattern) {
      this.pattern = pattern;
    }

    public int in(String s) {
      final int position = s.indexOf(pattern);
      if(position < 0)
        throw new IllegalArgumentException("String '" + pattern + "' not found in: '" + s + "'");
      return position;
    }

  }

}
