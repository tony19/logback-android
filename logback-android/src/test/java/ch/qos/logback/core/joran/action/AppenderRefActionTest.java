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

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link AppenderRefAction}.
 */
public class AppenderRefActionTest {

  Context context = new ContextBase();
  InterpretationContext ic;
  HashMap<String, Appender<Object>> appenderBag = new HashMap<String, Appender<Object>>();
  AppenderAttachableImpl<Object> attachable = new AppenderAttachableImpl<Object>();
  ListAppender<Object> listAppender = new ListAppender<Object>();
  AppenderRefAction<Object> action = new AppenderRefAction<Object>();
  DummyAttributes atts = new DummyAttributes();

  @Before
  public void setUp() {
    Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(9);
    interpreter.setDocumentLocator(locator);
    ic = interpreter.getInterpretationContext();
    ic.getObjectMap().put(ActionConst.APPENDER_BAG, appenderBag);
    action.setContext(context);

    listAppender.setName("A1");
    appenderBag.put("A1", listAppender);
  }

  @Test
  public void referencedAppenderIsAttachedToTheObjectOnTop() {
    context.putProperty("ref", "A1");
    ic.pushObject(attachable);
    atts.setValue(ActionConst.REF_ATTRIBUTE, "${ref}");

    action.begin(ic, "appender-ref", atts);

    assertFalse(action.inError);
    assertTrue(attachable.isAttached(listAppender));
    assertSame(attachable, ic.peekObject());
    assertStatus(Status.INFO, "Attaching appender named [A1] to " + attachable);
  }

  @Test
  public void objectOnTopThatCannotTakeAppendersIsAnError() {
    ic.pushObject("not an AppenderAttachable");
    atts.setValue(ActionConst.REF_ATTRIBUTE, "A1");

    action.begin(ic, "appender-ref", atts);

    assertTrue(action.inError);
    assertStatus(Status.ERROR,
        "Could not find an AppenderAttachable at the top of execution stack. Near [appender-ref] line 9");
    assertNull(attachable.getAppender("A1"));
  }

  @Test
  public void missingRefAttributeIsAnError() {
    ic.pushObject(attachable);

    action.begin(ic, "appender-ref", atts);

    assertTrue(action.inError);
    assertStatus(Status.ERROR, "Missing appender ref attribute in <appender-ref> tag.");
    assertFalse(attachable.iteratorForAppenders().hasNext());
  }

  @Test
  public void emptyRefAttributeIsAnError() {
    ic.pushObject(attachable);
    atts.setValue(ActionConst.REF_ATTRIBUTE, "");

    action.begin(ic, "appender-ref", atts);

    assertTrue(action.inError);
    assertStatus(Status.ERROR, "Missing appender ref attribute in <appender-ref> tag.");
    assertFalse(attachable.iteratorForAppenders().hasNext());
  }

  @Test
  public void unknownAppenderIsAnError() {
    ic.pushObject(attachable);
    atts.setValue(ActionConst.REF_ATTRIBUTE, "nope");

    action.begin(ic, "appender-ref", atts);

    assertTrue(action.inError);
    assertStatus(Status.ERROR,
        "Could not find an appender named [nope]. Did you define it below instead of above in the configuration file?");
    assertStatus(Status.ERROR, "See " + CoreConstants.CODES_URL + "#appender_order for more details.");
    assertFalse(attachable.iteratorForAppenders().hasNext());
  }

  @Test
  public void beginForgetsThePreviousError() {
    ic.pushObject(attachable);
    action.begin(ic, "appender-ref", atts); // no ref
    assertTrue(action.inError);

    atts.setValue(ActionConst.REF_ATTRIBUTE, "A1");
    action.begin(ic, "appender-ref", atts);

    assertFalse(action.inError);
    assertTrue(attachable.isAttached(listAppender));
  }

  @Test
  public void endChangesNothing() {
    ic.pushObject(attachable);
    int statusCount = context.getStatusManager().getCount();

    action.end(ic, "appender-ref");

    assertSame(attachable, ic.peekObject());
    assertEquals(statusCount, context.getStatusManager().getCount());
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
