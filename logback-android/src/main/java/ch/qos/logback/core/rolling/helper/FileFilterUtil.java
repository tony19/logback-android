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
package ch.qos.logback.core.rolling.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileFilterUtil {

  public static String afterLastSlash(String sregex) {
    int i = sregex.lastIndexOf('/');
    if (i == -1) {
      return sregex;
    } else {
      return sregex.substring(i + 1);
    }
  }

  /**
   * Return the set of files matching the stemRegex as found in 'directory'. A
   * stemRegex does not contain any slash characters or any folder separators.
   *
   * @param file folder's file object
   * @param stemRegex regex file pattern to match
   * @return matching files
   */
  public static File[] filesInFolderMatchingStemRegex(File file,
      final String stemRegex) {

    if (file == null) {
      return new File[0];
    }
    if (!file.exists() || !file.isDirectory()) {
      return new File[0];
    }
    return file.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.matches(stemRegex);
      }
    });
  }

  static public int findHighestCounter(File[] matchingFileArray, final String stemRegex) {
    int max = Integer.MIN_VALUE;
    for (File aFile : matchingFileArray) {
      int aCounter = FileFilterUtil.extractCounter(aFile, stemRegex);
      if (max < aCounter)
        max = aCounter;
    }
    return max;
  }

  static public int extractCounter(File file, final String stemRegex) {
    Pattern p = Pattern.compile(stemRegex);
    String lastFileName = file.getName();

    Matcher m = p.matcher(lastFileName);
    if (!m.matches()) {
      throw new IllegalStateException("The regex [" + stemRegex
          + "] should match [" + lastFileName + "]");
    }
    String counterAsStr = m.group(1);
    return Integer.valueOf(counterAsStr);
  }

  public static String slashify(String in) {
    return in.replace('\\', '/');
  }
}
