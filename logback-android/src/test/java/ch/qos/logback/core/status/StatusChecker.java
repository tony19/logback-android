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
package ch.qos.logback.core.status;

import ch.qos.logback.core.Context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Extend  StatusUtil with assertions.
 */
public class StatusChecker extends StatusUtil {

  public StatusChecker(StatusManager sm) {
    super(sm);
  }

  public StatusChecker(Context context) {
    super(context);
  }

  public void assertContainsMatch(int level, String regex) {
    assertTrue(containsMatch(level, regex));
  }

  public void assertNoMatch(String regex) {
    assertFalse(containsMatch(regex));
  }

  public void assertContainsMatch(String regex) {
    assertTrue(containsMatch(regex));
  }

  public void asssertContainsException(Class<?> scanExceptionClass) {
    assertTrue(containsException(scanExceptionClass));
  }

  public void assertIsErrorFree() {
    assertTrue(isErrorFree(0));
  }

  public void assertIsWarningOrErrorFree() {
    assertTrue(isWarningOrErrorFree(0));
  }
}
