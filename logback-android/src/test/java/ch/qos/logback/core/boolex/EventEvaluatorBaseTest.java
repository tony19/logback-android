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
package ch.qos.logback.core.boolex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EventEvaluatorBaseTest {

  static class ConstantEvaluator extends EventEvaluatorBase<Object> {
    @Override
    public boolean evaluate(Object event) {
      return true;
    }
  }

  private final ConstantEvaluator evaluator = new ConstantEvaluator();

  @Test
  public void nameIsNullUntilSet() {
    assertNull(evaluator.getName());
  }

  @Test
  public void setNameStoresTheName() {
    evaluator.setName("first");

    assertEquals("first", evaluator.getName());
  }

  @Test
  public void settingTheNameTwiceIsRejectedAndKeepsTheFirstName() {
    evaluator.setName("first");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> evaluator.setName("second"));

    assertEquals("name has been already set", e.getMessage());
    assertEquals("first", evaluator.getName());
  }

  @Test
  public void isNotStartedInitially() {
    assertFalse(evaluator.isStarted());
  }

  @Test
  public void startAndStopToggleStartedState() {
    evaluator.start();
    assertTrue(evaluator.isStarted());

    evaluator.stop();
    assertFalse(evaluator.isStarted());
  }
}
