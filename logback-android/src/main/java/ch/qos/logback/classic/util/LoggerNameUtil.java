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

import ch.qos.logback.core.CoreConstants;

/**
 * Utility class for analysing logger names.
 */
public class LoggerNameUtil {


  public static int getFirstSeparatorIndexOf(String name) {
    return getSeparatorIndexOf(name, 0);
  }

  /**
   * Get the position of the separator character, if any, starting at position
   * 'fromIndex'.
   *
   * @param name string to search
   * @param fromIndex starting search index
   * @return position of separator
   */
  public static int getSeparatorIndexOf(String name, int fromIndex) {
    int dotIndex = name.indexOf(CoreConstants.DOT, fromIndex);
    int dollarIndex = name.indexOf(CoreConstants.DOLLAR, fromIndex);

    if (dotIndex == -1 && dollarIndex == -1) return -1;
    if (dotIndex == -1) return dollarIndex;
    if (dollarIndex == -1) return dotIndex;

    return dotIndex < dollarIndex ? dotIndex : dollarIndex;
  }

}
