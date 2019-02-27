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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class FileSorter {

  private final List<FilenameParser> parsers;

  FileSorter(FilenameParser ...parsers) {
    this.parsers = Arrays.asList(parsers);
  }

  @SuppressWarnings("unchecked")
  void sort(String[] filenames) {
    Arrays.sort(filenames, new Comparator<String>() {
      @Override
      public int compare(String f1, String f2) {
        int result = 0;

        for (FilenameParser p : parsers) {
          Comparable c2 = p.parseFilename(f2);
          Comparable c1 = p.parseFilename(f1);
          if (c2 != null && c1 != null) {
            result += c2.compareTo(c1);
          }
        }

        // fallback to raw filename comparison
        if (result == 0) {
          result = f2.compareTo(f1);
        }

        return result;
      }
    });
  }

}
