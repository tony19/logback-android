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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

public class RootLoggerActionTest {

  private final LoggerContext context = new LoggerContext();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final RootLoggerAction action = new RootLoggerAction();
  private final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void beginSetsTheRootLevelAndPushesTheRootLogger() {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", ActionConst.LEVEL_ATTRIBUTE, ActionConst.LEVEL_ATTRIBUTE, "CDATA", "error");

    action.begin(ic, "root", attributes);

    assertFalse(action.inError);
    assertSame(root, ic.peekObject());
    assertEquals(Level.ERROR, root.getLevel());
  }

  @Test
  public void endPopsTheRootLogger() {
    action.begin(ic, "root", new AttributesImpl());

    action.end(ic, "root");

    assertTrue(ic.isEmpty());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void endAfterAnErrorLeavesTheStackAlone() {
    action.begin(ic, "root", new AttributesImpl());
    action.inError = true;

    action.end(ic, "root");

    assertSame(root, ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void beginClearsAPreviousError() {
    action.inError = true;

    action.begin(ic, "root", new AttributesImpl());

    assertFalse(action.inError);
  }

  @Test
  public void endWarnsWhenTheRootLoggerIsNotOnTopOfTheStack() {
    action.begin(ic, "root", new AttributesImpl());
    ic.pushObject("intruder");

    action.end(ic, "root");

    assertEquals("intruder", ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 2, statuses.size());
    assertEquals(Status.WARN, statuses.get(0).getLevel());
    assertEquals("The object on the top the of the stack is not the root logger", statuses.get(0).getMessage());
    assertEquals(Status.WARN, statuses.get(1).getLevel());
    assertEquals("It is: intruder", statuses.get(1).getMessage());
  }

  @Test
  public void finishLeavesTheStackAlone() {
    ic.pushObject(root);

    action.finish(ic);

    assertSame(root, ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }
}
