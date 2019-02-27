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

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Evaluates whether a given an event matches user-specified criteria.
 *
 * <p>
 * Implementations are free to evaluate the event as they see fit. In
 * particular, the evaluation results <em>may</em> depend on previous events.
 *
 * @author Ceki G&uuml;lc&uuml;
 */

public interface EventEvaluator<E> extends ContextAware, LifeCycle {

  /**
   * Evaluates whether the event passed as parameter matches some user-specified
   * criteria.
   *
   * <p>
   * The <code>Evaluator</code> is free to evaluate the event as it pleases. In
   * particular, the evaluation results <em>may</em> depend on previous events.
   *
   * @param event
   *          The event to evaluate
   * @return true if there is a match, false otherwise.
   * @throws NullPointerException
   *           can be thrown in presence of null values
   * @throws EvaluationException
   *           may be thrown during faulty evaluation
   */
  boolean evaluate(E event) throws NullPointerException, EvaluationException;

  /**
   * Gets this evaluator's name
   *
   * @return The name of this evaluator.
   */
  String getName();

  /**
   * Sets this evaluator's name
   * @param name the desired name
   */
  void setName(String name);
}
