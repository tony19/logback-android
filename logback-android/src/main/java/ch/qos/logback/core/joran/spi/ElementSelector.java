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
package ch.qos.logback.core.joran.spi;

import java.util.List;
import java.util.Locale;

/**
 * ElementSelector extends {@link ElementPath} with matching operations such as {@link #fullPathMatch(ElementPath)},
 * {@link #getPrefixMatchLength(ElementPath)} and {@link #getTailMatchLength(ElementPath)}.
 *
 * <p>Parts of the path may contain '*' for wildcard matching.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.1.0
 */
public class ElementSelector extends ElementPath {

  public ElementSelector() {
    super();
  }

  public ElementSelector(List<String> list) {
    super(list);
  }

  /**
   * Build an elementPath from a string.
   *
   * Note that "/x" is considered equivalent to "x" and to "x/"
   * @param p element path
   */
  public ElementSelector(String p) {
    super(p);
  }

  public boolean fullPathMatch(ElementPath path) {
    if (path.size() != size()) {
      return false;
    }

    int len = size();
    for (int i = 0; i < len; i++) {
      if (!equalityCheck(get(i), path.get(i))) {
        return false;
      }
    }
    // if everything matches, then the two patterns are equal
    return true;
  }

  /**
   * Returns the number of "tail" components that this pattern has in common
   * with the pattern p passed as parameter. By "tail" components we mean the
   * components at the end of the pattern.
   * @param p element path
   * @return the number of "tail" components in common with p
   */
  public int getTailMatchLength(ElementPath p) {
    if (p == null) {
      return 0;
    }

    int lSize = this.partList.size();
    int rSize = p.partList.size();

    // no match possible for empty sets
    if ((lSize == 0) || (rSize == 0)) {
      return 0;
    }

    int minLen = (lSize <= rSize) ? lSize : rSize;
    int match = 0;

    // loop from the end to the front
    for (int i = 1; i <= minLen; i++) {
      String l = this.partList.get(lSize - i);
      String r = p.partList.get(rSize - i);

      if (equalityCheck(l, r)) {
        match++;
      } else {
        break;
      }
    }
    return match;
  }

  public boolean isContainedIn(ElementPath p) {
    if(p == null) {
      return false;
    }
    return p.toStableString().contains(toStableString());
  }


  /**
   * Returns the number of "prefix" components that this pattern has in common
   * with the pattern p passed as parameter. By "prefix" components we mean the
   * components at the beginning of the pattern.
   * @param p element path
   * @return the number of "prefix" components in common with p
   */
  public int getPrefixMatchLength(ElementPath p) {
    if (p == null) {
      return 0;
    }

    int lSize = this.partList.size();
    int rSize = p.partList.size();

    // no match possible for empty sets
    if ((lSize == 0) || (rSize == 0)) {
      return 0;
    }

    int minLen = (lSize <= rSize) ? lSize : rSize;
    int match = 0;

    for (int i = 0; i < minLen; i++) {
      String l = this.partList.get(i);
      String r = p.partList.get(i);

      if (equalityCheck(l, r)) {
        match++;
      } else {
        break;
      }
    }

    return match;
  }

  private boolean equalityCheck(String x, String y) {
    return x.equalsIgnoreCase(y);
  }

  @Override
  public boolean equals(Object o) {
    if ((o == null) || !(o instanceof ElementSelector)) {
      return false;
    }

    ElementSelector r = (ElementSelector) o;

    if (r.size() != size()) {
      return false;
    }

    int len = size();

    for (int i = 0; i < len; i++) {
      if (!equalityCheck(get(i), r.get(i))) {
        return false;
      }
    }

    // if everything matches, then the two patterns are equal
    return true;
  }

  @Override
  public int hashCode() {
    int hc = 0;
    int len = size();

    for (int i = 0; i < len; i++) {
      // make Pattern comparisons case insensitive
      // http://jira.qos.ch/browse/LBCORE-76
      hc ^= get(i).toLowerCase(Locale.US).hashCode();
    }
    return hc;
  }


}
