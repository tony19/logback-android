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
package ch.qos.logback.core.pattern.util;

/**
 * @author Ceki G&uuml;lc&uuml;
 */
public class AsIsEscapeUtil implements IEscapeUtil {

  /**
   * Do not perform any character escaping.
   * <p>
   * Note that this method assumes that it is called after the escape character
   * has been consumed.
   */
  public void escape(String escapeChars, StringBuffer buf, char next,
                     int pointer) {
    // restitute the escape char (because it was consumed
    // before this method was called).
    buf.append("\\");
    // restitute the next character
    buf.append(next);
  }
}
