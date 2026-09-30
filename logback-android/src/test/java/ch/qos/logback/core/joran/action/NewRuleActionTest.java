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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.ext.HelloAction;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link NewRuleAction}.
 */
public class NewRuleActionTest {

  private final Context context = new ContextBase();
  private final RuleStore ruleStore = new SimpleRuleStore(context);
  private final InterpretationContext ic =
      new Interpreter(context, ruleStore, new ElementPath()).getInterpretationContext();
  private final NewRuleAction action = new NewRuleAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void ruleIsAddedToTheInterpretersRuleStore() {
    action.begin(ic, "newRule", newRuleAttributes("x/hello", HelloAction.class.getName()));

    assertFalse(action.inError);
    List<Action> actions = ruleStore.matchActions(new ElementPath("x/hello"));
    assertEquals(1, actions.size());
    assertTrue(actions.get(0) instanceof HelloAction);
    assertSame(context, actions.get(0).getContext());
    assertOnlyStatus(Status.INFO,
        "About to add new Joran parsing rule [x/hello," + HelloAction.class.getName() + "].");
  }

  @Test
  public void missingPatternIsReported() {
    action.begin(ic, "newRule", newRuleAttributes(null, HelloAction.class.getName()));

    assertTrue(action.inError);
    assertOnlyStatus(Status.ERROR, "No 'pattern' attribute in <newRule>");
  }

  @Test
  public void missingActionClassIsReported() {
    action.begin(ic, "newRule", newRuleAttributes("x/hello", ""));

    assertTrue(action.inError);
    assertNull(ruleStore.matchActions(new ElementPath("x/hello")));
    assertOnlyStatus(Status.ERROR, "No 'actionClass' attribute in <newRule>");
  }

  @Test
  public void failureToAddTheRuleIsReported() throws Exception {
    RuleStore failingStore = mock(RuleStore.class);
    doThrow(new ClassNotFoundException(HelloAction.class.getName()))
        .when(failingStore).addRule(any(ElementSelector.class), anyString());
    InterpretationContext failingIc =
        new Interpreter(context, failingStore, new ElementPath()).getInterpretationContext();

    action.begin(failingIc, "newRule", newRuleAttributes("x/hello", HelloAction.class.getName()));

    assertTrue(action.inError);
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(2, statuses.size());
    Status error = statuses.get(1);
    assertEquals(Status.ERROR, error.getLevel());
    assertEquals("Could not add new Joran parsing rule [x/hello," + HelloAction.class.getName() + "]",
        error.getMessage());
  }

  @Test
  public void previousErrorIsForgottenByTheNextElement() {
    action.begin(ic, "newRule", newRuleAttributes(null, null));
    assertTrue(action.inError);

    action.begin(ic, "newRule", newRuleAttributes("x/hello", HelloAction.class.getName()));

    assertFalse(action.inError);
  }

  @Test
  public void endAndFinishDoNothing() {
    ic.pushObject("top");

    action.end(ic, "newRule");
    action.finish(ic);

    assertSame("top", ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertEquals(0, context.getStatusManager().getCount());
  }

  private static DummyAttributes newRuleAttributes(String pattern, String actionClass) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.PATTERN_ATTRIBUTE, pattern);
    atts.setValue(Action.ACTION_CLASS_ATTRIBUTE, actionClass);
    return atts;
  }

  private void assertOnlyStatus(int level, String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(level, statuses.get(0).getLevel());
    assertEquals(message, statuses.get(0).getMessage());
  }
}
