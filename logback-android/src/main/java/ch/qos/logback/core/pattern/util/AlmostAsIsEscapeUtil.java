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

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.FileNamePattern;

/**
 * This implementation is intended for use in {@link FileNamePattern}.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class AlmostAsIsEscapeUtil extends RestrictedEscapeUtil {

  /**
   * Do not perform any character escaping, except for '%', and ')'.
   * 
   * <p>
   * Here is the rationale. First, filename patterns do not include escape
   * combinations such as \r or \n. Moreover, characters which have special
   * meaning in logback parsers, such as '{', or '}' cannot be part of file
   * names (so me thinks). The left parenthesis character has special meaning
   * only if it is preceded by %. Thus, the only characters that needs escaping
   * are '%' and ')'.
   * 
   * <p>
   * Note that this method assumes that it is called after the escape character
   * has been consumed.
   */
  public void escape(String escapeChars, StringBuffer buf, char next,
      int pointer) {
    super.escape(""+CoreConstants.PERCENT_CHAR+CoreConstants.RIGHT_PARENTHESIS_CHAR, buf, next, pointer);
  }
}
