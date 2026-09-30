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
package ch.qos.logback.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Test;

/**
 * Unit tests for {@link QueueFactory}.
 */
public class QueueFactoryTest {

  private final QueueFactory queueFactory = new QueueFactory();

  @Test
  public void createsEmptyDequeWithTheGivenCapacity() {
    LinkedBlockingDeque<String> deque = queueFactory.newLinkedBlockingDeque(5);

    assertEquals(0, deque.size());
    assertEquals(5, deque.remainingCapacity());
  }

  @Test
  public void usesCapacityOfOneWhenGivenCapacityIsZero() {
    LinkedBlockingDeque<String> deque = queueFactory.newLinkedBlockingDeque(0);

    assertEquals(1, deque.remainingCapacity());
  }

  @Test
  public void usesCapacityOfOneWhenGivenCapacityIsNegative() {
    LinkedBlockingDeque<String> deque = queueFactory.newLinkedBlockingDeque(-3);

    assertEquals(1, deque.remainingCapacity());
  }

  @Test
  public void createsNewDequeOnEachCall() {
    assertNotSame(queueFactory.newLinkedBlockingDeque(1), queueFactory.newLinkedBlockingDeque(1));
  }
}
