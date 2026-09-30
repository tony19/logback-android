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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RollingPolicyBaseTest {

  /** The smallest concrete rolling policy: everything under test lives in the base class. */
  static class MinimalRollingPolicy extends RollingPolicyBase {
    @Override
    public void rollover() {
    }

    @Override
    public String getActiveFileName() {
      return null;
    }
  }

  MinimalRollingPolicy policy = new MinimalRollingPolicy();

  @Test
  public void fileNamePatternIsUnsetByDefault() {
    assertNull(policy.getFileNamePattern());
  }

  @Test
  public void fileNamePatternIsTheOneSet() {
    policy.setFileNamePattern("logs/app-%d{yyyy-MM-dd}.log.gz");

    assertEquals("logs/app-%d{yyyy-MM-dd}.log.gz", policy.getFileNamePattern());
  }
}
