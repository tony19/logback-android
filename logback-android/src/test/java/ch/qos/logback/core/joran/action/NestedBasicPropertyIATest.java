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
import ch.qos.logback.core.util.AggregationType;

/**
 * Tests {@link NestedBasicPropertyIA} without an XML parser: the action is
 * driven directly with a bean on the interpretation context's object stack.
 */
public class NestedBasicPropertyIATest {

  private final Context context = new ContextBase();
  private final InterpretationContext ic = new InterpretationContext(context, null);
  private final NestedBasicPropertyIA action = new NestedBasicPropertyIA();
  private final BasicPropertyBean bean = new BasicPropertyBean();

  @Before
  public void setUp() {
    action.setContext(context);
  }

  @Test
  public void isNotApplicableWithoutParentObject() {
    assertFalse(action.isApplicable(new ElementPath("bean/count"), new DummyAttributes(), ic));
    assertTrue(action.actionDataStack.isEmpty());
  }

  @Test
  public void isNotApplicableToUnknownProperty() {
    assertNotApplicable("unknown");
  }

  @Test
  public void isNotApplicableToComplexProperty() {
    assertNotApplicable("child");
  }

  @Test
  public void isNotApplicableToComplexPropertyCollection() {
    assertNotApplicable("friend");
  }

  @Test
  public void isApplicableToBasicProperty() {
    assertApplicable("count", AggregationType.AS_BASIC_PROPERTY);
  }

  @Test
  public void isApplicableToBasicPropertyCollection() {
    assertApplicable("name", AggregationType.AS_BASIC_PROPERTY_COLLECTION);
  }

  private void assertNotApplicable(String tagName) {
    ic.pushObject(bean);

    assertFalse(action.isApplicable(new ElementPath("bean/" + tagName), new DummyAttributes(), ic));

    assertTrue(action.actionDataStack.isEmpty());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  private void assertApplicable(String tagName, AggregationType expectedType) {
    ic.pushObject(bean);

    assertTrue(action.isApplicable(new ElementPath("bean/" + tagName), new DummyAttributes(), ic));

    assertEquals(1, action.actionDataStack.size());
    IADataForBasicProperty data = action.actionDataStack.peek();
    assertSame(bean, data.parentBean.getObj());
    assertEquals(expectedType, data.aggregationType);
    assertEquals(tagName, data.propertyName);
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  /** A bean with one property of each {@link AggregationType}. */
  public static class BasicPropertyBean {
    int count;
    final List<String> names = new ArrayList<String>();
    BasicPropertyBean child;
    final List<BasicPropertyBean> friends = new ArrayList<BasicPropertyBean>();

    public void setCount(int count) {
      this.count = count;
    }

    public void addName(String name) {
      names.add(name);
    }

    public void setChild(BasicPropertyBean child) {
      this.child = child;
    }

    public void addFriend(BasicPropertyBean friend) {
      friends.add(friend);
    }
  }
}
