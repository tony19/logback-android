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

/**
 * Various utility methods for processing strings representing context types.
 * 
 * @author Ceki Gulcu
 * 
 */
public class ContentTypeUtil {

  public static boolean isTextual(String contextType) {
    if (contextType == null) {
      return false;
    }
    return contextType.startsWith("text");
  }

  public static String getSubType(String contextType) {
    if (contextType == null) {
      return null;
    }
    int index = contextType.indexOf('/');
    if (index == -1) {
      return null;
    } else {
      int subTypeStartIndex = index + 1;
      if (subTypeStartIndex < contextType.length()) {
        return contextType.substring(subTypeStartIndex);
      } else {
        return null;
      }
    }
  }
}
