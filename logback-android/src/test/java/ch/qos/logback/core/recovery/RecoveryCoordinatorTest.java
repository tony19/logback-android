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
package ch.qos.logback.core.recovery;

import static junit.framework.Assert.*;

import org.junit.Test;

public class RecoveryCoordinatorTest {

  long now = System.currentTimeMillis();
  RecoveryCoordinator rc = new RecoveryCoordinator(now);

  @Test
  public void recoveryNotNeededAfterInit() {
    RecoveryCoordinator rc = new RecoveryCoordinator();
    assertTrue(rc.isTooSoon());
  }

  @Test
  public void recoveryNotNeededIfAsleepForLessThanBackOffTime() throws InterruptedException {
    rc.setCurrentTime(now + RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN / 2);
    assertTrue(rc.isTooSoon());
  }

  @Test
  public void recoveryNeededIfAsleepForMoreThanBackOffTime() throws InterruptedException {
    rc.setCurrentTime(now + RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN + 20);
    assertFalse(rc.isTooSoon());
  }

  @Test
  public void recoveryNotNeededIfCurrentTimeSetToBackOffTime() throws InterruptedException {
    rc.setCurrentTime(now + RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN);
    assertTrue(rc.isTooSoon());
  }

  @Test
  public void recoveryNeededIfCurrentTimeSetToExceedBackOffTime() {
    rc.setCurrentTime(now + RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN + 1);
    assertFalse(rc.isTooSoon());
  }

  @Test
  public void recoveryConditionDetectedEvenAfterReallyLongTimesBetweenRecovery() {
    // Since backoff time quadruples whenever recovery is needed,
    // we double the offset on each for-loop iteration, causing
    // every other iteration to trigger recovery.

    long offset = RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN;

    for (int i = 0; i < 16; i++) {
      rc.setCurrentTime(now + offset);

      if (i % 2 == 0) {
        assertTrue("recovery should've been needed at " + offset, rc.isTooSoon());
      } else {
        assertFalse("recovery should NOT have been needed at " + offset, rc.isTooSoon());
      }
      offset *= 2;
    }
  }
}
