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
package ch.qos.logback.core.joran.spi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.xml.sax.Attributes;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.IncompatibleClassException;

/**
 * Test SimpleRuleStore for various explicit rule combinations.
 * 
 * We also test that explicit patterns are case sensitive.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class SimpleRuleStoreTest {

  SimpleRuleStore srs = new SimpleRuleStore(new ContextBase());
  CaseCombinator cc = new CaseCombinator();
  
  @Test
  public void smoke() throws Exception {
    srs.addRule(new ElementSelector("a/b"), new XAction());

    // test for all possible case combinations of "a/b"
    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(1, r.size());

      if (!(r.get(0) instanceof XAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void smokeII() throws Exception {
    srs.addRule(new ElementSelector("a/b"), new XAction());
    srs.addRule(new ElementSelector("a/b"), new YAction());

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(2, r.size());

      if (!(r.get(0) instanceof XAction)) {
        fail("Wrong type");
      }

      if (!(r.get(1) instanceof YAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void testSlashSuffix() throws Exception {
    ElementSelector pa = new ElementSelector("a/");
    srs.addRule(pa, new XAction());

    for (String s : cc.combinations("a")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(1, r.size());

      if (!(r.get(0) instanceof XAction)) {
        fail("Wrong type");
      }
    }

  }

  @Test
  public void testTail1() throws Exception {
    srs.addRule(new ElementSelector("*/b"), new XAction());

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);

      assertEquals(1, r.size());

      if (!(r.get(0) instanceof XAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void testTail2() throws Exception {
    SimpleRuleStore srs = new SimpleRuleStore(new ContextBase());
    srs.addRule(new ElementSelector("*/c"), new XAction());

    for (String s : cc.combinations("a/b/c")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);

      assertEquals(1, r.size());

      if (!(r.get(0) instanceof XAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void testTail3() throws Exception {
    srs.addRule(new ElementSelector("*/b"), new XAction());
    srs.addRule(new ElementSelector("*/a/b"), new YAction());

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(1, r.size());

      if (!(r.get(0) instanceof YAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void testTail4() throws Exception {
    srs.addRule(new ElementSelector("*/b"), new XAction());
    srs.addRule(new ElementSelector("*/a/b"), new YAction());
    srs.addRule(new ElementSelector("a/b"), new ZAction());

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(1, r.size());

      if (!(r.get(0) instanceof ZAction)) {
        fail("Wrong type");
      }
    }
  }

  @Test
  public void testSuffix() throws Exception {
    srs.addRule(new ElementSelector("a"), new XAction());
    srs.addRule(new ElementSelector("a/*"), new YAction());

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);
      assertEquals(1, r.size());
      assertTrue(r.get(0) instanceof YAction);
    }
  }

  @Test
  public void testDeepSuffix() throws Exception {
    srs.addRule(new ElementSelector("a"), new XAction(1));
    srs.addRule(new ElementSelector("a/b/*"), new XAction(2));

    for (String s : cc.combinations("a/other")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNull(r);
    }
  }

  @Test
  public void testPrefixSuffixInteraction1() throws Exception {
    srs.addRule(new ElementSelector("a"), new ZAction());
    srs.addRule(new ElementSelector("a/*"), new YAction());
    srs.addRule(new ElementSelector("*/a/b"), new XAction(3));

    for (String s : cc.combinations("a/b")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNotNull(r);

      assertEquals(1, r.size());

      assertTrue(r.get(0) instanceof XAction);
      XAction xaction = (XAction) r.get(0);
      assertEquals(3, xaction.id);
    }
  }

  @Test
  public void testPrefixSuffixInteraction2() throws Exception {
    srs.addRule(new ElementSelector("tG"), new XAction());
    srs.addRule(new ElementSelector("tG/tS"), new YAction());
    srs.addRule(new ElementSelector("tG/tS/test"), new ZAction());
    srs.addRule(new ElementSelector("tG/tS/test/*"), new XAction(9));

    for (String s : cc.combinations("tG/tS/toto")) {
      List<Action> r = srs.matchActions(new ElementPath(s));
      assertNull(r);
    }
  }

  @Test
  public void addRuleByClassNameInstantiatesTheActionWithTheStoreContext() {
    Context context = new ContextBase();
    SimpleRuleStore store = new SimpleRuleStore(context);
    store.addRule(new ElementSelector("a"), NOPAction.class.getName());

    List<Action> r = store.matchActions(new ElementPath("a"));
    assertNotNull(r);
    assertEquals(1, r.size());
    assertTrue(r.get(0) instanceof NOPAction);
    assertSame(context, r.get(0).getContext());
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void addRuleByClassNameReportsUninstantiableClassAndAddsNoRule() {
    Context context = new ContextBase();
    SimpleRuleStore store = new SimpleRuleStore(context);
    store.addRule(new ElementSelector("a"), String.class.getName());

    assertNull(store.matchActions(new ElementPath("a")));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Could not instantiate class [java.lang.String]", statuses.get(0).getMessage());
    assertTrue(statuses.get(0).getThrowable() instanceof IncompatibleClassException);
  }

  @Test
  public void middleMatchFindsSelectorContainedAnywhereInThePath() {
    srs.addRule(new ElementSelector("*/b/*"), new XAction(1));

    List<Action> r = srs.matchActions(new ElementPath("a/b/c"));
    assertNotNull(r);
    assertEquals(1, r.size());
    assertEquals(1, ((XAction) r.get(0)).id);

    assertNull(srs.matchActions(new ElementPath("a/x/c")));
  }

  @Test
  public void longestMiddleMatchWins() {
    srs.addRule(new ElementSelector("*/b/*"), new XAction(1));
    srs.addRule(new ElementSelector("*/b/c/*"), new XAction(2));
    srs.addRule(new ElementSelector("*/x/*"), new XAction(3));

    List<Action> r = srs.matchActions(new ElementPath("a/b/c/d"));
    assertNotNull(r);
    assertEquals(1, r.size());
    assertEquals(2, ((XAction) r.get(0)).id);
  }

  @Test
  public void starStarSelectorDoesNotMatchOrdinaryPaths() {
    // "*/*" has nothing between its two stars, so the middle part looked up
    // in the path is "*/*" itself
    srs.addRule(new ElementSelector("*/*"), new XAction(1));

    assertNull(srs.matchActions(new ElementPath("a/b/c")));
  }

  @Test
  public void loneKleeneStarMatchesNothing() {
    srs.addRule(new ElementSelector("*"), new XAction(1));

    assertNull(srs.matchActions(new ElementPath("a")));
    assertNull(srs.matchActions(new ElementPath("a/b")));
  }

  @Test
  public void toStringListsTheRules() {
    srs.addRule(new ElementSelector("a"), new XAction(7));

    assertEquals("SimpleRuleStore ( rules = {[a]=[XAction(7)]}   )", srs.toString());
  }

  class XAction extends Action {
    int id = 0;

    XAction() {
    }

    XAction(int id) {
      this.id = id;
    }

    public void begin(InterpretationContext ec, String name,
        Attributes attributes) {
    }

    public void end(InterpretationContext ec, String name) {
    }

    public void finish(InterpretationContext ec) {
    }

    public String toString() {
      return "XAction(" + id + ")";
    }
  }

  class YAction extends Action {
    public void begin(InterpretationContext ec, String name,
        Attributes attributes) {
    }

    public void end(InterpretationContext ec, String name) {
    }

    public void finish(InterpretationContext ec) {
    }
  }

  class ZAction extends Action {
    public void begin(InterpretationContext ec, String name,
        Attributes attributes) {
    }

    public void end(InterpretationContext ec, String name) {
    }

    public void finish(InterpretationContext ec) {
    }
  }

}
