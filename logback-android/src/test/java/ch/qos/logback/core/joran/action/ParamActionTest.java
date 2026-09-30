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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.status.Status;

/**
 * Tests {@link ParamAction}.
 */
public class ParamActionTest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final ParamAction action = new ParamAction();
  private final ParamActionBean bean = new ParamActionBean();

  @Before
  public void setUp() {
    action.setContext(context);
    ic.pushObject(bean);
  }

  @Test
  public void parameterIsSetOnTheObjectOnTopOfTheStack() {
    context.putProperty("prop", "greeting");
    context.putProperty("who", "world");

    action.begin(ic, "param", paramAttributes("${prop}", "  hello ${who}  "));

    assertEquals("hello world", bean.greeting);
    assertFalse(action.inError);
    assertSame(bean, ic.peekObject());
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void missingNameIsReported() {
    action.begin(ic, "param", paramAttributes(null, "hello"));

    assertTrue(action.inError);
    assertNull(bean.greeting);
    assertOnlyError(ParamAction.NO_NAME);
    assertEquals("No name attribute in <param> element", ParamAction.NO_NAME);
  }

  @Test
  public void missingValueIsReported() {
    action.begin(ic, "param", paramAttributes("greeting", null));

    assertTrue(action.inError);
    assertNull(bean.greeting);
    assertOnlyError(ParamAction.NO_VALUE);
    assertEquals("No value attribute in <param> element", ParamAction.NO_VALUE);
  }

  @Test
  public void endAndFinishDoNothing() {
    action.end(ic, "param");
    action.finish(ic);

    assertSame(bean, ic.peekObject());
    assertEquals(1, ic.getObjectStack().size());
    assertEquals(0, context.getStatusManager().getCount());
  }

  private static DummyAttributes paramAttributes(String name, String value) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.NAME_ATTRIBUTE, name);
    atts.setValue(Action.VALUE_ATTRIBUTE, value);
    return atts;
  }

  private void assertOnlyError(String message) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(statuses.toString(), 1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals(message, statuses.get(0).getMessage());
  }

  /** The object whose parameter is set. */
  public static class ParamActionBean {
    String greeting;

    public void setGreeting(String greeting) {
      this.greeting = greeting;
    }
  }
}
