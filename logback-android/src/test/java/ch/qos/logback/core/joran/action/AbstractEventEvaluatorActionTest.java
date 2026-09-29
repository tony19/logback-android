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
package ch.qos.logback.core.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.DynamicClassLoadingException;

/**
 * Tests {@link AbstractEventEvaluatorAction}.
 */
public class AbstractEventEvaluatorActionTest {

  static final String EVALUATOR_CLASS = TestEvaluator.class.getName();

  ContextBase context = new ContextBase();
  InterpretationContext ic = new InterpretationContext(context, null);
  TestEvaluatorAction action = new TestEvaluatorAction();
  DummyAttributes atts = new DummyAttributes();
  Map<String, EventEvaluator<?>> evaluatorMap = new HashMap<String, EventEvaluator<?>>();

  @Before
  public void setUp() {
    action.setContext(context);
    context.putObject(CoreConstants.EVALUATOR_MAP, evaluatorMap);
  }

  @Test
  public void beginPushesANamedEvaluatorOfTheGivenClass() {
    atts.setValue(Action.CLASS_ATTRIBUTE, EVALUATOR_CLASS);
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");

    action.begin(ic, "evaluator", atts);

    assertFalse(action.inError);
    TestEvaluator evaluator = (TestEvaluator) ic.peekObject();
    assertSame(action.evaluator, evaluator);
    assertEquals("myEval", evaluator.getName());
    assertSame(context, evaluator.getContext());
    assertFalse(evaluator.isStarted());
    assertStatus(Status.INFO, "Adding evaluator named [myEval] to the object stack");
    assertNoStatus("Assuming default evaluator class");
  }

  @Test
  public void beginFallsBackToTheDefaultClassWhenNoneIsGiven() {
    action.defaultClassName = EVALUATOR_CLASS;
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");

    action.begin(ic, "evaluator", atts);

    assertFalse(action.inError);
    assertTrue(ic.peekObject() instanceof TestEvaluator);
    assertStatus(Status.INFO, "Assuming default evaluator class [" + EVALUATOR_CLASS + "]");
  }

  @Test
  public void emptyClassAttributeFallsBackToTheDefaultClass() {
    action.defaultClassName = EVALUATOR_CLASS;
    atts.setValue(Action.CLASS_ATTRIBUTE, "");
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");

    action.begin(ic, "evaluator", atts);

    assertFalse(action.inError);
    assertTrue(ic.peekObject() instanceof TestEvaluator);
    assertStatus(Status.INFO, "Assuming default evaluator class [" + EVALUATOR_CLASS + "]");
  }

  @Test
  public void missingClassWithoutDefaultIsAnError() {
    action.defaultClassName = null;
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");

    action.begin(ic, "evaluator", atts);

    assertTrue(action.inError);
    assertNull(action.evaluator);
    assertTrue(ic.isEmpty());
    assertStatus(Status.ERROR, "Mandatory \"class\" attribute not set for <evaluator>");
  }

  @Test
  public void missingNameIsAnError() {
    atts.setValue(Action.CLASS_ATTRIBUTE, EVALUATOR_CLASS);

    action.begin(ic, "evaluator", atts);

    assertTrue(action.inError);
    assertNull(action.evaluator);
    assertTrue(ic.isEmpty());
    assertStatus(Status.ERROR, "Mandatory \"name\" attribute not set for <evaluator>");
  }

  @Test
  public void failedBeginForgetsThePreviousEvaluator() {
    beginWithValidEvaluator();
    assertTrue(action.evaluator instanceof TestEvaluator);

    atts.setValue(Action.NAME_ATTRIBUTE, "");
    action.begin(ic, "evaluator", atts);

    assertTrue(action.inError);
    assertNull(action.evaluator);
    assertStatus(Status.ERROR, "Mandatory \"name\" attribute not set for <evaluator>");
  }

  @Test
  public void uninstantiableClassIsAnError() {
    atts.setValue(Action.CLASS_ATTRIBUTE, "no.such.Evaluator");
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");

    action.begin(ic, "evaluator", atts);

    assertTrue(action.inError);
    assertTrue(ic.isEmpty());
    Status status = assertStatus(Status.ERROR, "Could not create evaluator of type no.such.Evaluator].");
    assertTrue(status.getThrowable() instanceof DynamicClassLoadingException);
  }

  @Test
  public void endAfterAFailedBeginDoesNothing() {
    action.begin(ic, "evaluator", atts); // neither class nor name
    int statusCount = context.getStatusManager().getCount();
    ic.pushObject("unrelated");

    action.end(ic, "evaluator");

    assertEquals(statusCount, context.getStatusManager().getCount());
    assertEquals("unrelated", ic.peekObject());
    assertTrue(evaluatorMap.isEmpty());
  }

  @Test
  public void endStartsAndRegistersTheEvaluator() {
    TestEvaluator evaluator = beginWithValidEvaluator();

    action.end(ic, "evaluator");

    assertTrue(evaluator.isStarted());
    assertTrue(ic.isEmpty());
    assertSame(evaluator, evaluatorMap.get("myEval"));
    assertStatus(Status.INFO, "Starting evaluator named [myEval]");
  }

  @Test
  public void endLeavesTheStackAloneWhenTheEvaluatorIsNotOnTop() {
    TestEvaluator evaluator = beginWithValidEvaluator();
    ic.pushObject("intruder");

    action.end(ic, "evaluator");

    assertTrue(evaluator.isStarted());
    assertEquals("intruder", ic.popObject());
    assertSame(evaluator, ic.popObject());
    assertTrue(evaluatorMap.isEmpty());
    assertStatus(Status.WARN, "The object on the top the of the stack is not the evaluator pushed earlier.");
  }

  @Test
  public void endWithoutEvaluatorMapIsAnError() {
    context.removeObject(CoreConstants.EVALUATOR_MAP);
    TestEvaluator evaluator = beginWithValidEvaluator();

    action.end(ic, "evaluator");

    assertTrue(evaluator.isStarted());
    assertTrue(ic.isEmpty());
    assertStatus(Status.ERROR, "Could not find EvaluatorMap");
  }

  @Test
  public void failureToRegisterTheEvaluatorIsAnError() {
    context.putObject(CoreConstants.EVALUATOR_MAP, Collections.<String, EventEvaluator<?>>emptyMap());
    TestEvaluator evaluator = beginWithValidEvaluator();

    action.end(ic, "evaluator");

    assertTrue(ic.isEmpty());
    Status status = assertStatus(Status.ERROR, "Could not set evaluator named [" + evaluator + "].");
    assertTrue(status.getThrowable() instanceof UnsupportedOperationException);
  }

  @Test
  public void beginForgetsThePreviousError() {
    action.begin(ic, "evaluator", atts); // neither class nor name
    assertTrue(action.inError);

    TestEvaluator evaluator = beginWithValidEvaluator();
    action.end(ic, "evaluator");

    assertTrue(evaluator.isStarted());
    assertSame(evaluator, evaluatorMap.get("myEval"));
  }

  @Test
  public void finishLeavesTheInterpretationContextUnchanged() {
    TestEvaluator evaluator = beginWithValidEvaluator();
    int statusCount = context.getStatusManager().getCount();

    action.finish(ic);

    assertSame(evaluator, ic.peekObject());
    assertFalse(evaluator.isStarted());
    assertEquals(statusCount, context.getStatusManager().getCount());
  }

  private TestEvaluator beginWithValidEvaluator() {
    atts.setValue(Action.CLASS_ATTRIBUTE, EVALUATOR_CLASS);
    atts.setValue(Action.NAME_ATTRIBUTE, "myEval");
    action.begin(ic, "evaluator", atts);
    assertFalse(action.inError);
    return (TestEvaluator) ic.peekObject();
  }

  private Status assertStatus(int level, String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    for (Status s : statuses) {
      if (s.getLevel() == level && message.equals(s.getMessage())) {
        return s;
      }
    }
    throw new AssertionError("no status of level " + level + " with message [" + message + "] in " + statuses);
  }

  private void assertNoStatus(String messagePrefix) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      assertFalse(s.getMessage(), s.getMessage().startsWith(messagePrefix));
    }
  }

  static class TestEvaluatorAction extends AbstractEventEvaluatorAction {
    String defaultClassName;

    @Override
    protected String defaultClassName() {
      return defaultClassName;
    }
  }

  public static class TestEvaluator extends EventEvaluatorBase<Object> {
    @Override
    public boolean evaluate(Object event) throws EvaluationException {
      return true;
    }
  }
}
