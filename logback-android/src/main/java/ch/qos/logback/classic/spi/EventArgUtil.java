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
package ch.qos.logback.classic.spi;

public class EventArgUtil {


  public static final Throwable extractThrowable(Object[] argArray) {
    if (argArray == null || argArray.length == 0) {
      return null;
    }

    final Object lastEntry = argArray[argArray.length - 1];
    if (lastEntry instanceof Throwable) {
      return (Throwable) lastEntry;
    }
    return null;
  }

  /**
   * This method should be called only if {@link #successfulExtraction(Throwable)} returns true.
   *
   * @param argArray array to copy
   * @return copy of array with one less element
   */
  public static Object[] trimmedCopy(Object[] argArray) {
    if (argArray == null || argArray.length == 0) {
      throw new IllegalStateException("non-sensical empty or null argument array");
    }
    final int trimemdLen = argArray.length - 1;
    Object[] trimmed = new Object[trimemdLen];
    System.arraycopy(argArray, 0, trimmed, 0, trimemdLen);
    return trimmed;
  }

  public static boolean successfulExtraction(Throwable throwable) {
    return throwable != null;
  }
}
