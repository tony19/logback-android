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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link ConversionRuleAction}.
 */
public class ConversionRuleActionTest {

  static final String CONVERTER_CLASS = "com.example.MyConverter";

  Context context = new ContextBase();
  InterpretationContext ic = new InterpretationContext(context, null);
  ConversionRuleAction action = new ConversionRuleAction();
  DummyAttributes atts = new DummyAttributes();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void firstRuleCreatesTheRuleRegistry() {
    assertNull(context.getObject(CoreConstants.PATTERN_RULE_REGISTRY));
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);

    action.begin(ic, "conversionRule", atts);

    assertFalse(action.inError);
    assertEquals(Collections.singletonMap("my", CONVERTER_CLASS), ruleRegistry());
    assertStatus(Status.INFO, "registering conversion word my with class [" + CONVERTER_CLASS + "]");
  }

  @Test
  public void laterRulesAreAddedToTheExistingRegistry() {
    Map<String, String> registry = new HashMap<String, String>();
    registry.put("other", "com.example.OtherConverter");
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);

    action.begin(ic, "conversionRule", atts);

    assertFalse(action.inError);
    assertSame(registry, ruleRegistry());
    assertEquals(2, registry.size());
    assertEquals(CONVERTER_CLASS, registry.get("my"));
    assertEquals("com.example.OtherConverter", registry.get("other"));
  }

  @Test
  public void missingConversionWordIsAnError() {
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);

    action.begin(ic, "conversionRule", atts);

    assertTrue(action.inError);
    assertNull(ruleRegistry());
    assertStatus(Status.ERROR, "No 'conversionWord' attribute in <conversionRule>");
  }

  @Test
  public void missingConverterClassIsAnError() {
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");

    action.begin(ic, "conversionRule", atts);

    assertTrue(action.inError);
    assertNull(ruleRegistry());
    assertStatus(Status.ERROR, "No 'converterClass' attribute in <conversionRule>");
  }

  @Test
  public void emptyConversionWordIsAnError() {
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);

    action.begin(ic, "conversionRule", atts);

    assertTrue(action.inError);
    assertNull(ruleRegistry());
    assertStatus(Status.ERROR, "No 'conversionWord' attribute in <conversionRule>");
  }

  @Test
  public void emptyConverterClassIsAnError() {
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, "");

    action.begin(ic, "conversionRule", atts);

    assertTrue(action.inError);
    assertNull(ruleRegistry());
    assertStatus(Status.ERROR, "No 'converterClass' attribute in <conversionRule>");
  }

  @Test
  public void failureToRegisterTheRuleIsAnError() {
    Map<String, String> readOnly = Collections.emptyMap();
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, readOnly);
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);

    action.begin(ic, "conversionRule", atts);

    assertTrue(action.inError);
    assertTrue(readOnly.isEmpty());
    assertStatus(Status.ERROR, "Could not add conversion rule to PatternLayout.");
  }

  @Test
  public void beginForgetsThePreviousError() {
    action.begin(ic, "conversionRule", atts); // no attributes at all
    assertTrue(action.inError);

    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);
    action.begin(ic, "conversionRule", atts);

    assertFalse(action.inError);
    assertEquals(CONVERTER_CLASS, ruleRegistry().get("my"));
  }

  @Test
  public void endAndFinishChangeNothing() {
    atts.setValue(ActionConst.CONVERSION_WORD_ATTRIBUTE, "my");
    atts.setValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE, CONVERTER_CLASS);
    action.begin(ic, "conversionRule", atts);
    int statusCount = context.getStatusManager().getCount();

    action.end(ic, "conversionRule");
    action.finish(ic);

    assertFalse(action.inError);
    assertEquals(Collections.singletonMap("my", CONVERTER_CLASS), ruleRegistry());
    assertTrue(ic.isEmpty());
    assertEquals(statusCount, context.getStatusManager().getCount());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> ruleRegistry() {
    return (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
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
}
