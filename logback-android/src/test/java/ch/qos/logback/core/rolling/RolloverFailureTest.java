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
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.Test;

public class RolloverFailureTest {

  @Test
  public void messageOnlyFailureHasNoCause() {
    RolloverFailure failure = new RolloverFailure("rename failed");

    assertEquals("rename failed", failure.getMessage());
    assertNull(failure.getCause());
  }

  @Test
  public void failureKeepsMessageAndCause() {
    IOException cause = new IOException("disk full");

    RolloverFailure failure = new RolloverFailure("rename failed", cause);

    assertEquals("rename failed", failure.getMessage());
    assertSame(cause, failure.getCause());
  }
}
