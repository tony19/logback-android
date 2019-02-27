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

import java.util.ArrayList;
import java.util.List;

public class CaseCombinator {


  List<String> combinations(String in) {
    int length = in.length();
    List<String> permutationsList = new ArrayList<String>();

    int totalCombinations = computeTotalNumerOfCombinations(in, length);

    for (int j = 0; j < totalCombinations; j++) {
      StringBuilder newCombination = new StringBuilder();
      int pos = 0;
      for (int i = 0; i < length; i++) {
        char c = in.charAt(i);
        if (isEnglishLetter(c)) {
          c = permute(c, j, pos);
          pos++;
        }
        newCombination.append(c);
      }
      permutationsList.add(newCombination.toString());
    }
    return permutationsList;

  }

  private char permute(char c, int permutation, int position) {
    int mask = 1 << position;
    boolean shouldBeInUpperCase = (permutation & mask) != 0;
    boolean isEffectivelyUpperCase = isUpperCase(c);
    if (shouldBeInUpperCase && !isEffectivelyUpperCase)
      return toUpperCase(c);
    if (!shouldBeInUpperCase && isEffectivelyUpperCase)
      return toLowerCase(c);
    return c;
  }

  private int computeTotalNumerOfCombinations(String in, int length) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      char c = in.charAt(i);
      if (isEnglishLetter(c))
        count++;
    }
    // return 2^count (2 to the power of count)
    return (1 << count);
  }

  private char toUpperCase(char c) {
    if ('A' <= c && c <= 'Z') {
      return c;
    }
    if ('a' <= c && c <= 'z') {
      return (char) ((int) c + 'A' - 'a');
    }
    // code should never reach this point
    return c;
  }

  private char toLowerCase(char c) {
    if ('a' <= c && c <= 'z') {
      return c;
    }
    if ('A' <= c && c <= 'Z') {
      return (char) ((int) c + 'a' - 'A');
    }
    // code should never reach this point
    return c;
  }

  private boolean isEnglishLetter(char c) {
    if ('a' <= c && c <= 'z')
      return true;

    if ('A' <= c && c <= 'Z')
      return true;
    return false;
  }

  private boolean isUpperCase(char c) {
    return ('A' <= c && c <= 'Z');
  }
}
