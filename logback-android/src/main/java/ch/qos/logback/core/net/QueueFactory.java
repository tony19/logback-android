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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Factory for {@link java.util.Queue} instances.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class QueueFactory {

  /**
   * Creates a new {@link LinkedBlockingDeque} with the given {@code capacity}.
   * In case the given capacity is smaller than one it will automatically be
   * converted to one.
   *
   * @param capacity the capacity to use for the queue
   * @param <E> the type of elements held in the queue
   * @return a new instance of {@link ArrayBlockingQueue}
   */
  public <E> LinkedBlockingDeque<E> newLinkedBlockingDeque(int capacity) {
    final int actualCapacity = capacity < 1 ? 1 : capacity;
    return new LinkedBlockingDeque<E>(actualCapacity);
  }
}
