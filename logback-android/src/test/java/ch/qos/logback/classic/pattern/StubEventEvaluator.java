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
package ch.qos.logback.classic.pattern;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.boolex.EventEvaluatorBase;

/**
 * Evaluator for the converter tests of this package: it returns a fixed result
 * (or throws a fixed exception) and counts its invocations.
 */
class StubEventEvaluator extends EventEvaluatorBase<ILoggingEvent> {

  private final boolean result;
  private final EvaluationException failure;
  int invocations;

  StubEventEvaluator(String name, boolean result) {
    this(name, result, null);
  }

  StubEventEvaluator(String name, EvaluationException failure) {
    this(name, false, failure);
  }

  private StubEventEvaluator(String name, boolean result, EvaluationException failure) {
    setName(name);
    this.result = result;
    this.failure = failure;
  }

  /**
   * Makes this evaluator known under its name in the context's evaluator map,
   * where converters look up the evaluators named in their options.
   */
  @SuppressWarnings("unchecked")
  StubEventEvaluator registerIn(Context context) {
    Map<String, EventEvaluator<?>> evaluatorMap =
        (Map<String, EventEvaluator<?>>) context.getObject(CoreConstants.EVALUATOR_MAP);
    evaluatorMap.put(getName(), this);
    return this;
  }

  @Override
  public boolean evaluate(ILoggingEvent event) throws EvaluationException {
    invocations++;
    if (failure != null) {
      throw failure;
    }
    return result;
  }
}
