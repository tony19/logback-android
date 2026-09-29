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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

@SuppressWarnings("deprecation")
public class LevelActionTest {

  private final LoggerContext context = new LoggerContext();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final LevelAction action = new LevelAction();
  private final Logger logger = context.getLogger("a.b");

  @Before
  public void setUp() {
    action.setContext(context);
    logger.setLevel(Level.WARN);
  }

  @Test
  public void levelOutsideOfALoggerIsAnError() {
    ic.pushObject("not a logger");

    action.begin(ic, "level", value("INFO"));

    assertTrue(action.inError);
    assertEquals("not a logger", ic.peekObject());
    assertEquals(Level.WARN, logger.getLevel());
    assertOnlyStatus(Status.ERROR, "For element <level>, could not find a logger at the top of execution stack.");
  }

  @Test
  public void valueSetsTheLevelOfTheEnclosingLogger() {
    ic.pushObject(logger);

    action.begin(ic, "level", value("INFO"));

    assertFalse(action.inError);
    assertEquals(Level.INFO, logger.getLevel());
    assertOnlyStatus(Status.INFO, "a.b level set to INFO");
  }

  @Test
  public void unknownValueSetsDebug() {
    ic.pushObject(logger);

    action.begin(ic, "level", value("LOUD"));

    assertEquals(Level.DEBUG, logger.getLevel());
  }

  @Test
  public void inheritedValueMakesTheLevelInherited() {
    ic.pushObject(logger);

    action.begin(ic, "level", value("inherited"));

    assertNull(logger.getLevel());
    assertOnlyStatus(Status.INFO, "a.b level set to null");
  }

  @Test
  public void nullValueMakesTheLevelInherited() {
    ic.pushObject(logger);

    action.begin(ic, "level", value("Null"));

    assertNull(logger.getLevel());
    assertOnlyStatus(Status.INFO, "a.b level set to null");
  }

  @Test
  public void endAndFinishLeaveTheStackAndLoggerAlone() {
    ic.pushObject(logger);

    action.end(ic, "level");
    action.finish(ic);

    assertSame(logger, ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertEquals(Level.WARN, logger.getLevel());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  private static AttributesImpl value(String value) {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", ActionConst.VALUE_ATTR, ActionConst.VALUE_ATTR, "CDATA", value);
    return attributes;
  }

  private void assertOnlyStatus(int level, String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(level, statuses.get(0).getLevel());
    assertEquals(message, statuses.get(0).getMessage());
  }
}
