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
package ch.qos.logback.classic.pattern;

import ch.qos.logback.core.CoreConstants;

/**
 * This abbreviator returns the class name from a fully qualified class name,
 * removing the leading package name.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class ClassNameOnlyAbbreviator implements Abbreviator {

  public String abbreviate(String fqClassName) {
    // we ignore the fact that the separator character can also be a dollar
    // If the inner class is org.good.AClass#Inner, returning
    // AClass#Inner seems most appropriate
    int lastIndex = fqClassName.lastIndexOf(CoreConstants.DOT);
    if (lastIndex != -1) {
      return fqClassName.substring(lastIndex + 1, fqClassName.length());
    } else {
      return fqClassName;
    }
  }
}
