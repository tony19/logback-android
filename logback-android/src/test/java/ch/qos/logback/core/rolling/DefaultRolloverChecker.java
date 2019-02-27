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
package ch.qos.logback.core.rolling;


import ch.qos.logback.core.util.Compare;
import ch.qos.logback.core.util.CoreTestConstants;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class DefaultRolloverChecker implements RolloverChecker {

  final String testId;
  final boolean withCompression;
  final String compressionSuffix;

  public DefaultRolloverChecker(String testId, boolean withCompression, String compressionSuffix) {
    this.testId = testId;
    this.withCompression = withCompression;
    this.compressionSuffix = compressionSuffix;
  }

  public void check(List<String> expectedFilenameList) throws IOException {

    int i = 0;
    for (String fn : expectedFilenameList) {
      String suffix = withCompression ? addGZIfNotLast(expectedFilenameList, i, compressionSuffix) : "";

      String witnessFileName = CoreTestConstants.TEST_DIR_PREFIX + "witness/rolling/tbr-" + testId + "." + i + suffix;
      assertTrue(Compare.compare(fn, witnessFileName));
      i++;
    }
  }

  String addGZIfNotLast(List<String> expectedFilenameList, int i, String suff) {
    int lastIndex = expectedFilenameList.size() - 1;
    return (i != lastIndex) ? suff : "";
  }
}


