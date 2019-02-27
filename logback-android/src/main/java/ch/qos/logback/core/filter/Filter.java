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
package ch.qos.logback.core.filter;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Users should extend this class to implement customized event filtering.
 *
 * <p>We suggest that you first try to use the built-in rules before rushing to
 * write your own custom filters.
 *
 * <p>For more information about filters, please refer to the online manual at
 * http://logback.qos.ch/manual/filters.html
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public abstract class Filter<E> extends ContextAwareBase implements LifeCycle {

  private String name;

  boolean start = false;

  public void start() {
    this.start = true;
  }

  public boolean isStarted() {
    return this.start;
  }

  public void stop() {
    this.start = false;
  }

  /**
   * If the decision is <code>{@link FilterReply#DENY}</code>, then the event will be
   * dropped. If the decision is <code>{@link FilterReply#NEUTRAL}</code>, then the next
   * filter, if any, will be invoked. If the decision is
   * <code>{@link FilterReply#ACCEPT}</code> then the event will be logged without
   * consulting with other filters in the chain.
   *
   * @param event
   *                The event to decide upon.
   * @return filter result (ACCEPT, DENY, NEUTRAL)
   */
  public abstract FilterReply decide(E event);

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
