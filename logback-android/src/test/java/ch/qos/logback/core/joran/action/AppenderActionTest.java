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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.DynamicClassLoadingException;

/**
 * Tests {@link AppenderAction}.
 */
public class AppenderActionTest {

  static final String LIST_APPENDER_CLASS = ListAppender.class.getName();

  Context context = new ContextBase();
  InterpretationContext ic;
  HashMap<String, Appender<Object>> appenderBag = new HashMap<String, Appender<Object>>();
  AppenderAction<Object> action = new AppenderAction<Object>();
  DummyAttributes atts = new DummyAttributes();

  @Before
  public void setUp() {
    Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(7);
    interpreter.setDocumentLocator(locator);
    ic = interpreter.getInterpretationContext();
    ic.getObjectMap().put(ActionConst.APPENDER_BAG, appenderBag);
    action.setContext(context);
  }

  @Test
  public void beginPushesAndBagsANamedAppenderOfTheGivenClass() throws ActionException {
    context.putProperty("appenderName", "A1");
    atts.setValue(Action.CLASS_ATTRIBUTE, LIST_APPENDER_CLASS);
    atts.setValue(Action.NAME_ATTRIBUTE, "${appenderName}");

    action.begin(ic, "appender", atts);

    Appender<?> appender = (Appender<?>) ic.peekObject();
    assertTrue(appender instanceof ListAppender);
    assertEquals("A1", appender.getName());
    assertSame(context, appender.getContext());
    assertFalse(appender.isStarted());
    assertSame(appender, appenderBag.get("A1"));
    assertStatus(Status.INFO, "About to instantiate appender of type [" + LIST_APPENDER_CLASS + "]");
    assertStatus(Status.INFO, "Naming appender as [A1]");
    assertNoStatusAtOrAbove(Status.WARN);
  }

  @Test
  public void appenderWithoutNameIsWarnedAboutAndBaggedUnderNull() throws ActionException {
    atts.setValue(Action.CLASS_ATTRIBUTE, LIST_APPENDER_CLASS);

    action.begin(ic, "appender", atts);

    Appender<?> appender = (Appender<?>) ic.peekObject();
    assertNull(appender.getName());
    assertSame(appender, appenderBag.get(null));
    assertStatus(Status.WARN, "No appender name given for appender of type " + LIST_APPENDER_CLASS + "].");
  }

  @Test
  public void consoleAppenderIsWarnedAboutAsDeprecated() throws ActionException {
    atts.setValue(Action.CLASS_ATTRIBUTE, "ch.qos.logback.core.ConsoleAppender");
    atts.setValue(Action.NAME_ATTRIBUTE, "console");

    action.begin(ic, "appender", atts);

    assertEquals("ch.qos.logback.core.ConsoleAppender", ic.peekObject().getClass().getName());
    assertStatus(Status.WARN, "ConsoleAppender is deprecated for LogcatAppender");
  }

  @Test
  public void missingClassIsAnErrorThatDisablesEnd() throws ActionException {
    atts.setValue(Action.NAME_ATTRIBUTE, "A1");

    action.begin(ic, "appender", atts);

    assertTrue(ic.isEmpty());
    assertTrue(appenderBag.isEmpty());
    assertStatus(Status.ERROR, "Missing class name for appender. Near [appender] line 7");

    int statusCount = context.getStatusManager().getCount();
    ic.pushObject("unrelated");
    action.end(ic, "appender");
    assertEquals("unrelated", ic.peekObject());
    assertEquals(statusCount, context.getStatusManager().getCount());
  }

  @Test
  public void failedBeginForgetsThePreviousAppender() throws ActionException {
    Appender<?> appender = beginWithListAppender();
    assertSame(appender, action.appender);

    atts.setValue(Action.CLASS_ATTRIBUTE, "");
    action.begin(ic, "appender", atts);

    assertNull(action.appender);
    assertStatus(Status.ERROR, "Missing class name for appender. Near [appender] line 7");
    action.end(ic, "appender");
    assertFalse(appender.isStarted());
  }

  @Test
  public void uninstantiableClassIsAnErrorThatAbortsTheElement() {
    atts.setValue(Action.CLASS_ATTRIBUTE, "no.such.Appender");
    atts.setValue(Action.NAME_ATTRIBUTE, "A1");

    ActionException e = assertThrows(ActionException.class, () -> action.begin(ic, "appender", atts));

    assertTrue(e.getCause() instanceof DynamicClassLoadingException);
    assertTrue(ic.isEmpty());
    assertTrue(appenderBag.isEmpty());
    Status status = assertStatus(Status.ERROR, "Could not create an Appender of type [no.such.Appender].");
    assertSame(e.getCause(), status.getThrowable());

    // the failed element leaves nothing for end() to do
    int statusCount = context.getStatusManager().getCount();
    action.end(ic, "appender");
    assertTrue(ic.isEmpty());
    assertEquals(statusCount, context.getStatusManager().getCount());
  }

  @Test
  public void missingAppenderBagIsAnError() {
    ic.getObjectMap().remove(ActionConst.APPENDER_BAG);
    atts.setValue(Action.CLASS_ATTRIBUTE, LIST_APPENDER_CLASS);
    atts.setValue(Action.NAME_ATTRIBUTE, "A1");

    ActionException e = assertThrows(ActionException.class, () -> action.begin(ic, "appender", atts));

    assertTrue(e.getCause() instanceof NullPointerException);
    assertTrue(ic.isEmpty());
    assertStatus(Status.ERROR, "Could not create an Appender of type [" + LIST_APPENDER_CLASS + "].");
  }

  @Test
  public void endStartsAndPopsTheAppender() throws ActionException {
    Appender<?> appender = beginWithListAppender();

    action.end(ic, "appender");

    assertTrue(appender.isStarted());
    assertTrue(ic.isEmpty());
    assertNoStatusAtOrAbove(Status.WARN);
  }

  @Test
  public void endLeavesTheStackAloneWhenTheAppenderIsNotOnTop() throws ActionException {
    Appender<?> appender = beginWithListAppender();
    ic.pushObject("intruder");

    action.end(ic, "appender");

    assertTrue(appender.isStarted());
    assertEquals("intruder", ic.popObject());
    assertSame(appender, ic.popObject());
    assertStatus(Status.WARN, "The object at the of the stack is not the appender named [A1] pushed earlier.");
  }

  @Test
  public void beginForgetsThePreviousError() throws ActionException {
    action.begin(ic, "appender", atts); // no class

    Appender<?> appender = beginWithListAppender();
    action.end(ic, "appender");

    assertTrue(appender.isStarted());
    assertTrue(ic.isEmpty());
  }

  private Appender<?> beginWithListAppender() throws ActionException {
    atts.setValue(Action.CLASS_ATTRIBUTE, LIST_APPENDER_CLASS);
    atts.setValue(Action.NAME_ATTRIBUTE, "A1");
    action.begin(ic, "appender", atts);
    return (Appender<?>) ic.peekObject();
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

  private void assertNoStatusAtOrAbove(int level) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      assertTrue(s.toString(), s.getLevel() < level);
    }
  }
}
