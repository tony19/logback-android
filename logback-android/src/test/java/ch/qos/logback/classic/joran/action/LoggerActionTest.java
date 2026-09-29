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
package ch.qos.logback.classic.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.status.Status;

public class LoggerActionTest {

  private final LoggerContext context = new LoggerContext();
  private final Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
  private final InterpretationContext ic = interpreter.getInterpretationContext();
  private final LoggerAction action = new LoggerAction();
  private final AttributesImpl attributes = new AttributesImpl();

  @Before
  public void setUp() {
    action.setContext(context);
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(12);
    locator.setColumnNumber(5);
    interpreter.setDocumentLocator(locator);
  }

  @Test
  public void missingNameIsReportedWithTheLocation() {
    Object parent = new Object();
    ic.pushObject(parent);

    action.begin(ic, "logger", attributes);

    assertTrue(action.inError);
    assertNull(action.logger);
    assertSame(parent, ic.peekObject());
    assertOnlyStatus(Status.ERROR, "No 'name' attribute in element logger, around line: 12, column: 5");
  }

  @Test
  public void endAfterAnErrorLeavesTheStackAlone() {
    Object parent = new Object();
    ic.pushObject(parent);
    action.begin(ic, "logger", attributes);

    action.end(ic, "logger");

    assertSame(parent, ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertEquals(1, context.getStatusManager().getCopyOfStatusList().size());
  }

  @Test
  public void beginClearsAPreviousError() {
    action.begin(ic, "logger", attributes);
    assertTrue(action.inError);

    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    action.begin(ic, "logger", attributes);

    assertFalse(action.inError);
    assertSame(context.getLogger("a.b"), ic.peekObject());
  }

  @Test
  public void levelAndAdditivityAreSet() {
    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    addAttribute(LoggerAction.LEVEL_ATTRIBUTE, "info");
    addAttribute(ActionConst.ADDITIVITY_ATTRIBUTE, "false");

    action.begin(ic, "logger", attributes);

    Logger logger = context.getLogger("a.b");
    assertEquals(Level.INFO, logger.getLevel());
    assertFalse(logger.isAdditive());
  }

  @Test
  public void inheritedLevelMakesTheLevelInherited() {
    Logger logger = context.getLogger("a.b");
    logger.setLevel(Level.WARN);
    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    addAttribute(LoggerAction.LEVEL_ATTRIBUTE, "Inherited");

    action.begin(ic, "logger", attributes);

    assertNull(logger.getLevel());
    assertOnlyStatus(Status.INFO, "Setting level of logger [a.b] to null, i.e. INHERITED");
  }

  @Test
  public void nullLevelMakesTheLevelInherited() {
    Logger logger = context.getLogger("a.b");
    logger.setLevel(Level.WARN);
    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    addAttribute(LoggerAction.LEVEL_ATTRIBUTE, "null");

    action.begin(ic, "logger", attributes);

    assertNull(logger.getLevel());
    assertOnlyStatus(Status.INFO, "Setting level of logger [a.b] to null, i.e. INHERITED");
  }

  @Test
  public void endPopsTheLogger() {
    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    action.begin(ic, "logger", attributes);
    assertSame(context.getLogger("a.b"), ic.peekObject());

    action.end(ic, "logger");

    assertTrue(ic.isEmpty());
  }

  @Test
  public void endWarnsWhenTheLoggerIsNotOnTopOfTheStack() {
    addAttribute(Action.NAME_ATTRIBUTE, "a.b");
    action.begin(ic, "logger", attributes);
    ic.pushObject("intruder");

    action.end(ic, "logger");

    assertEquals("intruder", ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 2, statuses.size());
    assertEquals(Status.WARN, statuses.get(0).getLevel());
    assertEquals("The object on the top the of the stack is not Logger[a.b] pushed earlier",
        statuses.get(0).getMessage());
    assertEquals(Status.WARN, statuses.get(1).getLevel());
    assertEquals("It is: intruder", statuses.get(1).getMessage());
  }

  @Test
  public void finishLeavesTheStackAlone() {
    ic.pushObject("parent");

    action.finish(ic);

    assertEquals("parent", ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  private void addAttribute(String name, String value) {
    attributes.addAttribute("", name, name, "CDATA", value);
  }

  private void assertOnlyStatus(int level, String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(level, statuses.get(0).getLevel());
    assertEquals(message, statuses.get(0).getMessage());
  }
}
