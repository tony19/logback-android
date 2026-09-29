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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EvaluatorFilterTest {

  static final String FAIL = "fail";

  /**
   * Matches the event "match", throws an {@link EvaluationException} for the
   * event {@link #FAIL} and does not match anything else.
   */
  static class EvaluatorFilterTestEvaluator extends EventEvaluatorBase<String> {
    final EvaluationException failure = new EvaluationException("boom");

    @Override
    public boolean evaluate(String event) throws EvaluationException {
      if (FAIL.equals(event)) {
        throw failure;
      }
      return "match".equals(event);
    }
  }

  private final Context context = new ContextBase();
  private final StatusChecker checker = new StatusChecker(context);
  private final EvaluatorFilter<String> filter = new EvaluatorFilter<String>();
  private final EvaluatorFilterTestEvaluator evaluator = new EvaluatorFilterTestEvaluator();

  @Before
  public void setUp() {
    filter.setContext(context);
    filter.setName("evalFilter");
    filter.setOnMatch(FilterReply.ACCEPT);
    filter.setOnMismatch(FilterReply.DENY);
    evaluator.setContext(context);
    evaluator.setName("myEvaluator");
  }

  @Test
  public void evaluatorIsNullUntilSet() {
    assertNull(filter.getEvaluator());
  }

  @Test
  public void setEvaluatorIsReturnedByGetter() {
    filter.setEvaluator(evaluator);

    assertSame(evaluator, filter.getEvaluator());
  }

  @Test
  public void startWithoutEvaluatorReportsErrorAndDoesNotStart() {
    filter.start();

    assertFalse(filter.isStarted());
    checker.assertContainsMatch(Status.ERROR, "No evaluator set for filter evalFilter");
  }

  @Test
  public void startWithEvaluatorStartsWithoutErrors() {
    filter.setEvaluator(evaluator);

    filter.start();

    assertTrue(filter.isStarted());
    checker.assertIsErrorFree();
  }

  @Test
  public void decideIsNeutralWhenFilterIsNotStarted() {
    filter.setEvaluator(evaluator);
    evaluator.start();

    assertEquals(FilterReply.NEUTRAL, filter.decide("match"));
    assertEquals(FilterReply.NEUTRAL, filter.decide("other"));
  }

  @Test
  public void decideIsNeutralWhenEvaluatorIsNotStarted() {
    filter.setEvaluator(evaluator);
    filter.start();

    assertEquals(FilterReply.NEUTRAL, filter.decide("match"));
    assertEquals(FilterReply.NEUTRAL, filter.decide("other"));
  }

  @Test
  public void decideReturnsOnMatchWhenEvaluatorMatches() {
    startFilterAndEvaluator();

    assertEquals(FilterReply.ACCEPT, filter.decide("match"));
  }

  @Test
  public void decideReturnsOnMismatchWhenEvaluatorDoesNotMatch() {
    startFilterAndEvaluator();

    assertEquals(FilterReply.DENY, filter.decide("other"));
  }

  @Test
  public void decideIsNeutralAndReportsErrorWhenEvaluatorThrows() {
    startFilterAndEvaluator();

    assertEquals(FilterReply.NEUTRAL, filter.decide(FAIL));

    checker.assertContainsMatch(Status.ERROR, "Evaluator myEvaluator threw an exception");
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertSame(evaluator.failure, statuses.get(statuses.size() - 1).getThrowable());
  }

  private void startFilterAndEvaluator() {
    filter.setEvaluator(evaluator);
    evaluator.start();
    filter.start();
  }
}
