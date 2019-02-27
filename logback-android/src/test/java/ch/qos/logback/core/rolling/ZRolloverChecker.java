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

public class ZRolloverChecker implements RolloverChecker {

  String testId;

  public ZRolloverChecker(String testId) {
    this.testId = testId;
  }

  public void check(List<String> expectedFilenameList) throws IOException {
    int lastIndex = expectedFilenameList.size() - 1;
    String lastFile = expectedFilenameList.get(lastIndex);
    String witnessFileName = CoreTestConstants.TEST_DIR_PREFIX + "witness/rolling/tbr-" + testId;
    assertTrue(Compare.compare(lastFile, witnessFileName));
  }
}
