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
import ch.qos.logback.core.PropertyDefinerBase;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;

/**
 * Plain-JVM tests (no Robolectric) of {@link DefinePropertyAction}, driving
 * begin() and end() directly. The tests that parse configuration files are in
 * {@link DefinePropertyActionTest}.
 */
public class DefinePropertyActionJvmTest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final DefinePropertyAction action = new DefinePropertyAction();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void definerValueIsDefinedAsLocalProperty() throws Exception {
    action.begin(ic, "define", defineAttributes("foo", AsLowerCasePropertyDefiner.class, null));

    AsLowerCasePropertyDefiner definer = (AsLowerCasePropertyDefiner) ic.peekObject();
    assertSame(context, definer.getContext());
    definer.setValue("MONSTER");
    action.end(ic, "define");

    assertEquals("monster", ic.getProperty("foo"));
    assertNull(context.getProperty("foo"));
    assertTrue(ic.isEmpty());
  }

  @Test
  public void lifeCycleDefinerIsStartedBeforeItsValueIsRead() throws Exception {
    action.begin(ic, "define", defineAttributes("foo", DefinePropertyActionDefiner.class, null));

    DefinePropertyActionDefiner definer = (DefinePropertyActionDefiner) ic.peekObject();
    assertTrue(definer.isStarted());
    assertSame(context, definer.getContext());

    definer.setValue("started");
    action.end(ic, "define");

    assertEquals("started", ic.getProperty("foo"));
    assertTrue(ic.isEmpty());
  }

  @Test
  public void nullValueDefinesNoProperty() throws Exception {
    action.begin(ic, "define", defineAttributes("foo", DefinePropertyActionDefiner.class, "context"));

    action.end(ic, "define");

    assertTrue(ic.isEmpty());
    assertFalse(context.getCopyOfPropertyMap().containsKey("foo"));
    assertNull(ic.getProperty("foo"));
  }

  @Test
  public void valueIsDefinedInTheRequestedScope() throws Exception {
    action.begin(ic, "define", defineAttributes("foo", DefinePropertyActionDefiner.class, "context"));
    ((DefinePropertyActionDefiner) ic.peekObject()).setValue("in context");

    action.end(ic, "define");

    assertEquals("in context", context.getProperty("foo"));
  }

  @Test
  public void foreignObjectOnTopOfTheStackIsReportedAndNoPropertyIsDefined() throws Exception {
    action.begin(ic, "define", defineAttributes("foo", DefinePropertyActionDefiner.class, null));
    ((DefinePropertyActionDefiner) ic.peekObject()).setValue("ignored");
    Object foreign = new Object();
    ic.pushObject(foreign);

    action.end(ic, "define");

    assertSame(foreign, ic.peekObject());
    assertEquals(2, ic.getObjectStack().size());
    assertNull(ic.getProperty("foo"));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    Status last = statuses.get(statuses.size() - 1);
    assertEquals(Status.WARN, last.getLevel());
    assertEquals("The object at the of the stack is not the property definer for property named [foo] pushed earlier.",
        last.getMessage());
  }

  private static DummyAttributes defineAttributes(String name, Class<?> definerClass, String scope) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.NAME_ATTRIBUTE, name);
    atts.setValue(Action.CLASS_ATTRIBUTE, definerClass.getName());
    atts.setValue(Action.SCOPE_ATTRIBUTE, scope);
    return atts;
  }

  /** A property definer with a life cycle; its value is null until set. */
  public static class DefinePropertyActionDefiner extends PropertyDefinerBase implements LifeCycle {
    private String value;
    private boolean started;

    public void setValue(String value) {
      this.value = value;
    }

    public String getPropertyValue() {
      return value;
    }

    public void start() {
      started = true;
    }

    public void stop() {
      started = false;
    }

    public boolean isStarted() {
      return started;
    }
  }
}
