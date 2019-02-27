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
package ch.qos.logback.core.pattern;

public class SpacePadder {

  final static String[] SPACES = { " ", "  ", "    ", "        ", // 1,2,4,8
      // spaces
      "                ", // 16 spaces
      "                                " }; // 32 spaces

  final static public void leftPad(StringBuilder buf, String s, int desiredLength) {
    int actualLen = 0;
    if (s != null) {
      actualLen = s.length();
    }
    if (actualLen < desiredLength) {
      spacePad(buf, desiredLength - actualLen);
    }
    if (s != null) {
      buf.append(s);
    }
  }

  final static public void rightPad(StringBuilder buf, String s, int desiredLength) {
    int actualLen = 0;
    if (s != null) {
      actualLen = s.length();
    }
    if (s != null) {
      buf.append(s);
    }
    if (actualLen < desiredLength) {
      spacePad(buf, desiredLength - actualLen);
    }
  }

  /**
   * Fast space padding method.
   * @param sbuf buffer to modify
   * @param length number of spaces to add
   */
  final static public void spacePad(StringBuilder sbuf, int length) {
    while (length >= 32) {
      sbuf.append(SPACES[5]);
      length -= 32;
    }

    for (int i = 4; i >= 0; i--) {
      if ((length & (1 << i)) != 0) {
        sbuf.append(SPACES[i]);
      }
    }
  }
}
