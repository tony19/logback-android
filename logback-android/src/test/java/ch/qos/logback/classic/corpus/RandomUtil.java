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
package ch.qos.logback.classic.corpus;

import java.util.Random;

public class RandomUtil {

  
  /**
   * Approximate a gaussian distrib with only positive integer values
   * 
   * @param average
   * @param stdDeviation
   * @return
   */
  static public int gaussianAsPositiveInt(Random random, int average, int stdDeviation) {
    if (average < 1) {
      throw new IllegalArgumentException(
          "The average must not be smaller than 1.");
    }

    if (stdDeviation < 1) {
      throw new IllegalArgumentException(
          "The stdDeviation must not be smaller than 1.");
    }

    double d = random.nextGaussian() * stdDeviation + average;
    int result = 1;
    if (d > 1.0) {
      result = (int) Math.round(d);
    }
    return result;
  }
}
