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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IntParser implements FilenameParser<Integer> {

  private final Pattern pathPattern;

  IntParser(FileNamePattern fileNamePattern) {
    String pathRegexString = fileNamePattern.toRegex(false, true);
    pathRegexString = FileFinder.unescapePath(pathRegexString);
    this.pathPattern = Pattern.compile(pathRegexString);
  }

  public Integer parseFilename(String filename) {
    Integer intValue = -1;

    try {
      intValue = Integer.parseInt(findToken(filename), 10);
    } catch (NumberFormatException e) {
      // ignore
    }

    return intValue;
  }

  private String findToken(String input) {
    Matcher m = this.pathPattern.matcher(input);
    return (m.find() && m.groupCount() >= 1) ? m.group(1) : "";
  }
}
