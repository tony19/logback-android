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
package ch.qos.logback.core.helpers;

import java.util.LinkedList;
import java.util.List;

import ch.qos.logback.core.CoreConstants;

public class ThrowableToStringArray {

  public static String[] convert(Throwable t) {
    List<String> strList = new LinkedList<String>();
    extract(strList, t, null);
    return strList.toArray(new String[0]);

  }

  private static void extract(List<String> strList, Throwable t,
      StackTraceElement[] parentSTE) {

    StackTraceElement[] ste = t.getStackTrace();
    final int numberOfcommonFrames = findNumberOfCommonFrames(ste, parentSTE);

    strList.add(formatFirstLine(t, parentSTE));
    for (int i = 0; i < (ste.length - numberOfcommonFrames); i++) {
      strList.add("\tat "+ste[i].toString());
    }

    if (numberOfcommonFrames != 0) {
      strList.add("\t... "+numberOfcommonFrames + " common frames omitted");
    }

    Throwable cause = t.getCause();
    if (cause != null) {
      ThrowableToStringArray.extract(strList, cause, ste);
    }
  }

  private static String formatFirstLine(Throwable t,
      StackTraceElement[] parentSTE) {
    String prefix = "";
    if (parentSTE != null) {
      prefix = CoreConstants.CAUSED_BY;
    }

    String result = prefix + t.getClass().getName();
    if (t.getMessage() != null) {
      result += ": " + t.getMessage();
    }
    return result;
  }

  private static int findNumberOfCommonFrames(StackTraceElement[] ste,
      StackTraceElement[] parentSTE) {
    if (parentSTE == null) {
      return 0;
    }

    int steIndex = ste.length - 1;
    int parentIndex = parentSTE.length - 1;
    int count = 0;
    while (steIndex >= 0 && parentIndex >= 0) {
      if (ste[steIndex].equals(parentSTE[parentIndex])) {
        count++;
      } else {
        break;
      }
      steIndex--;
      parentIndex--;
    }
    return count;
  }

}
