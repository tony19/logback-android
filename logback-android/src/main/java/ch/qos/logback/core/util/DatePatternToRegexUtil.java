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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is concerned with computing a regex corresponding to a date
 * pattern (in {@link SimpleDateFormat} format).
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class DatePatternToRegexUtil {

  final String datePattern;
  final int datePatternLength;
  final CharSequenceToRegexMapper regexMapper = new CharSequenceToRegexMapper();

  public DatePatternToRegexUtil(String datePattern) {
    this.datePattern = datePattern;
    datePatternLength = datePattern.length();
  }

  public String toRegex() {
    List<CharSequenceState> charSequenceList = tokenize();
    StringBuilder sb = new StringBuilder();
    for (CharSequenceState seq : charSequenceList) {
      sb.append(regexMapper.toRegex(seq));
    }
    return sb.toString();
  }

  private List<CharSequenceState> tokenize() {
    List<CharSequenceState> sequenceList = new ArrayList<CharSequenceState>();

    CharSequenceState lastCharSequenceState = null;

    for (int i = 0; i < datePatternLength; i++) {
      char t = datePattern.charAt(i);
      if (lastCharSequenceState == null || lastCharSequenceState.c != t) {
        lastCharSequenceState = new CharSequenceState(t);
        sequenceList.add(lastCharSequenceState);
      } else {
        lastCharSequenceState.incrementOccurrences();
      }
    }
    return sequenceList;
  }
}
