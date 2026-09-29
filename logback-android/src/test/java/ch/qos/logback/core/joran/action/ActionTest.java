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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;

/**
 * Tests the location helpers of {@link Action}.
 */
public class ActionTest {

  Context context = new ContextBase();
  Interpreter interpreter;
  InterpretationContext ic;
  LocatingAction action = new LocatingAction();

  @Before
  public void setUp() {
    interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
    ic = interpreter.getInterpretationContext();
    action.setContext(context);
  }

  @Test
  public void lineAndColumnComeFromTheInterpreterLocator() {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(12);
    locator.setColumnNumber(34);
    interpreter.setDocumentLocator(locator);

    assertEquals(12, action.getLineNumber(ic));
    assertEquals(34, action.getColumnNumber(ic));
    assertEquals("line: 12, column: 34", action.getLineColStr(ic));
  }

  @Test
  public void lineAndColumnAreMinusOneWithoutLocator() {
    interpreter.setDocumentLocator(null);

    assertEquals(-1, action.getLineNumber(ic));
    assertEquals(-1, action.getColumnNumber(ic));
    assertEquals("line: -1, column: -1", action.getLineColStr(ic));
  }

  @Test
  public void bodyIsIgnoredByDefault() throws Exception {
    ic.pushObject("top");

    action.body(ic, "some text");

    assertEquals("top", ic.popObject());
    assertTrue(ic.isEmpty());
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void toStringIsTheClassName() {
    assertEquals(LocatingAction.class.getName(), action.toString());
  }

  static class LocatingAction extends Action {
    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) {
    }

    @Override
    public void end(InterpretationContext ic, String name) {
    }
  }
}
