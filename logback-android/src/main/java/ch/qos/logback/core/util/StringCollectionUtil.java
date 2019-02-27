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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static utility methods for working with collections of strings.
 *
 * @author Carl Harris
 */
public class StringCollectionUtil {

  /**
   * Retains all values in the subject collection that are matched by
   * at least one of a collection of regular expressions.
   * <p>
   * This method is a convenience overload for
   * {@link #retainMatching(Collection, Collection)}.
   *
   * @param values subject value collection
   * @param patterns patterns to match
   */
  public static void retainMatching(Collection<String> values,
      String... patterns) {
    retainMatching(values, Arrays.asList(patterns));
  }

  /**
   * Retains all values in the subject collection that are matched by
   * at least one of a collection of regular expressions.
   * <p>
   * The semantics of this method are conceptually similar to
   * {@link Collection#retainAll(Collection)}, but uses pattern matching
   * instead of exact matching.
   *
   * @param values subject value collection
   * @param patterns patterns to match
   */
  public static void retainMatching(Collection<String> values,
      Collection<String> patterns) {
    if (patterns.isEmpty()) return;
    List<String> matches = new ArrayList<String>(values.size());
    for (String p : patterns) {
      Pattern pattern = Pattern.compile(p);
      for (String value : values) {
        if (pattern.matcher(value).matches()) {
          matches.add(value);
        }
      }
    }
    values.retainAll(matches);
  }

  /**
   * Removes all values in the subject collection that are matched by
   * at least one of a collection of regular expressions.
   * <p>
   * This method is a convenience overload for
   * {@link #removeMatching(Collection, Collection)}.
   *
   * @param values subject value collection
   * @param patterns patterns to match
   */
  public static void removeMatching(Collection<String> values,
      String... patterns) {
    removeMatching(values, Arrays.asList(patterns));
  }

  /**
   * Removes all values in the subject collection that are matched by
   * at least one of a collection of regular expressions.
   * <p>
   * The semantics of this method are conceptually similar to
   * {@link Collection#removeAll(Collection)}, but uses pattern matching
   * instead of exact matching.
   *
   * @param values subject value collection
   * @param patterns patterns to match
   */
  public static void removeMatching(Collection<String> values,
      Collection<String> patterns) {
    List<String> matches = new ArrayList<String>(values.size());
    for (String p : patterns) {
      Pattern pattern = Pattern.compile(p);
      for (String value : values) {
        if (pattern.matcher(value).matches()) {
          matches.add(value);
        }
      }
    }
    values.removeAll(matches);
  }

}
