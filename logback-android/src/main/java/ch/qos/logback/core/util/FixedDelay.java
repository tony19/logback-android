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
package ch.qos.logback.core.util;

/**
 * A default {@link DelayStrategy} that implements a simple fixed delay.
 *
 * @author Carl Harris
 * @since 1.1.0
 */
public class FixedDelay implements DelayStrategy {

  private final long subsequentDelay;
  private long nextDelay;

  /**
   * Initialize a new {@code FixedDelay} with a given {@code initialDelay} and
   * {@code subsequentDelay}.
   *
   * @param initialDelay    value for the initial delay
   * @param subsequentDelay value for all other delays
   */
  public FixedDelay(long initialDelay, long subsequentDelay) {
    this.nextDelay = initialDelay;
    this.subsequentDelay = subsequentDelay;
  }

  /**
   * Initialize a new {@code FixedDelay} with fixed delay value given by {@code delay}
   * parameter.
   *
   * @param delay value for all delays
   */
  public FixedDelay(int delay) {
    this(delay, delay);
  }

  /**
   * {@inheritDoc}
   */
  public long nextDelay() {
    long delay = nextDelay;
    nextDelay = subsequentDelay;
    return delay;
  }

}
