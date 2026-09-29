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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.joran.util.PropertySetter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.AggregationType;

/**
 * Tests {@link NestedComplexPropertyIA} without an XML parser: the action is
 * driven directly with a bean on the interpretation context's object stack.
 */
public class NestedComplexPropertyIATest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final NestedComplexPropertyIA action = new NestedComplexPropertyIA();
  private final ComplexPropertyParent parent = new ComplexPropertyParent();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void isNotApplicableWithoutParentObject() {
    assertFalse(action.isApplicable(new ElementPath("parent/child"), new DummyAttributes(), ic));
    assertTrue(action.actionDataStack.isEmpty());
  }

  @Test
  public void isNotApplicableToUnknownProperty() {
    assertNotApplicable("unknown");
  }

  @Test
  public void isNotApplicableToBasicProperty() {
    assertNotApplicable("count");
  }

  @Test
  public void isNotApplicableToBasicPropertyCollection() {
    assertNotApplicable("name");
  }

  @Test
  public void isApplicableToComplexProperty() {
    assertApplicable("child", AggregationType.AS_COMPLEX_PROPERTY);
  }

  @Test
  public void isApplicableToComplexPropertyCollection() {
    assertApplicable("friend", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
  }

  @Test
  public void declaredClassIsInstantiatedConfiguredStartedAndSetOnTheParent() {
    context.putProperty("childClass", ComplexPropertyChild.class.getName());
    DummyAttributes atts = classAttribute("${childClass}");
    enter("child", atts);

    action.begin(ic, "child", atts);

    ComplexPropertyChild child = (ComplexPropertyChild) ic.peekObject();
    assertSame(context, child.getContext());
    assertFalse(child.isStarted());
    assertNull(parent.child);

    action.end(ic, "child");

    assertSame(child, parent.child);
    assertSame(parent, child.parent);
    assertTrue(child.isStarted());
    assertSame(parent, ic.peekObject());
    assertTrue(action.actionDataStack.isEmpty());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void componentMarkedNoAutoStartIsNotStarted() {
    DummyAttributes atts = classAttribute(NoAutoStartChild.class.getName());
    enter("child", atts);

    action.begin(ic, "child", atts);
    action.end(ic, "child");

    assertTrue(parent.child instanceof NoAutoStartChild);
    assertFalse(parent.child.isStarted());
    assertSame(parent, parent.child.parent);
  }

  @Test
  public void classIsGuessedFromTheAdderAndTheComponentIsAddedToTheCollection() {
    DummyAttributes atts = new DummyAttributes();
    enter("friend", atts);

    action.begin(ic, "friend", atts);
    Object friend = ic.peekObject();
    action.end(ic, "friend");

    assertTrue(friend instanceof ComplexPropertyFriend);
    assertEquals(1, parent.friends.size());
    assertSame(friend, parent.friends.get(0));
    assertSame(parent, ic.peekObject());
    assertStatus(Status.INFO, "Assuming default type [" + ComplexPropertyFriend.class.getName()
        + "] for [friend] property");
  }

  @Test
  public void missingClassIsReportedAndTheElementIsIgnored() {
    DummyAttributes atts = new DummyAttributes();
    enter("helper", atts);

    action.begin(ic, "helper", atts);

    assertSame(parent, ic.peekObject());
    assertStatus(Status.ERROR, "Could not find an appropriate class for property [helper]");

    action.end(ic, "helper");

    assertNull(parent.helper);
    assertSame(parent, ic.peekObject());
    assertTrue(action.actionDataStack.isEmpty());
    assertEquals(1, context.getStatusManager().getCount());
  }

  @Test
  public void uninstantiableClassIsReportedAndTheElementIsIgnored() {
    DummyAttributes atts = classAttribute("no.such.Component");
    enter("child", atts);

    action.begin(ic, "child", atts);

    assertSame(parent, ic.peekObject());
    Status error = assertStatus(Status.ERROR,
        "Could not create component [child] of type [no.such.Component]");
    assertTrue(error.getThrowable() instanceof ClassNotFoundException);

    action.end(ic, "child");

    assertNull(parent.child);
    assertSame(parent, ic.peekObject());
    assertTrue(action.actionDataStack.isEmpty());
    assertEquals(1, context.getStatusManager().getCount());
  }

  @Test
  public void endReportsAForeignObjectOnTopOfTheStack() {
    DummyAttributes atts = classAttribute(ComplexPropertyChild.class.getName());
    enter("child", atts);
    action.begin(ic, "child", atts);
    Object foreign = new Object();
    ic.pushObject(foreign);

    action.end(ic, "child");

    assertNull(parent.child);
    assertSame(foreign, ic.peekObject());
    assertEquals(3, ic.getObjectStack().size());
    assertStatus(Status.ERROR,
        "The object on the top the of the stack is not the component pushed earlier.");
  }

  @Test
  public void endReportsActionDataOfAnUnexpectedAggregationType() {
    ic.pushObject(parent);
    action.actionDataStack.push(new IADataForComplexProperty(new PropertySetter(parent),
        AggregationType.AS_BASIC_PROPERTY, "child"));
    action.begin(ic, "child", classAttribute(ComplexPropertyChild.class.getName()));

    action.end(ic, "child");

    assertNull(parent.child);
    assertSame(parent, ic.peekObject());
    assertTrue(action.actionDataStack.isEmpty());
    assertStatus(Status.ERROR, "Unexpected aggregationType AS_BASIC_PROPERTY");
  }

  private void enter(String tagName, DummyAttributes atts) {
    ic.pushObject(parent);
    assertTrue(action.isApplicable(new ElementPath("parent/" + tagName), atts, ic));
  }

  private static DummyAttributes classAttribute(String className) {
    DummyAttributes atts = new DummyAttributes();
    atts.setValue(Action.CLASS_ATTRIBUTE, className);
    return atts;
  }

  private Status assertStatus(int level, String message) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (message.equals(s.getMessage())) {
        assertEquals(level, s.getLevel());
        return s;
      }
    }
    throw new AssertionError("no status [" + message + "] in "
        + context.getStatusManager().getCopyOfStatusList());
  }

  private void assertNotApplicable(String tagName) {
    ic.pushObject(parent);

    assertFalse(action.isApplicable(new ElementPath("parent/" + tagName), new DummyAttributes(), ic));

    assertTrue(action.actionDataStack.isEmpty());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  private void assertApplicable(String tagName, AggregationType expectedType) {
    ic.pushObject(parent);

    assertTrue(action.isApplicable(new ElementPath("parent/" + tagName), new DummyAttributes(), ic));

    assertEquals(1, action.actionDataStack.size());
    IADataForComplexProperty data = action.actionDataStack.peek();
    assertSame(parent, data.parentBean.getObj());
    assertEquals(expectedType, data.getAggregationType());
    assertEquals(tagName, data.getComplexPropertyName());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  /** A parent bean with one property of each {@link AggregationType}. */
  public static class ComplexPropertyParent {
    int count;
    final List<String> names = new ArrayList<String>();
    ComplexPropertyChild child;
    final List<ComplexPropertyFriend> friends = new ArrayList<ComplexPropertyFriend>();
    ComplexPropertyHelper helper;

    public void setCount(int count) {
      this.count = count;
    }

    public void addName(String name) {
      names.add(name);
    }

    public void setChild(ComplexPropertyChild child) {
      this.child = child;
    }

    public void addFriend(ComplexPropertyFriend friend) {
      friends.add(friend);
    }

    public void setHelper(ComplexPropertyHelper helper) {
      this.helper = helper;
    }
  }

  /** A context-aware, startable nested component that points back to its parent. */
  public static class ComplexPropertyChild extends ContextAwareBase implements LifeCycle {
    ComplexPropertyParent parent;
    private boolean started;

    public void setParent(ComplexPropertyParent parent) {
      this.parent = parent;
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

  /** A nested component that must not be started automatically. */
  @NoAutoStart
  public static class NoAutoStartChild extends ComplexPropertyChild {
  }

  /** A plain nested component: neither context-aware nor startable, no parent. */
  public static class ComplexPropertyFriend {
  }

  /** A property type Joran cannot instantiate without an explicit class. */
  public interface ComplexPropertyHelper {
  }
}
