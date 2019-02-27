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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;

import ch.qos.logback.core.joran.spi.ElementSelector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.SimpleConfigurator;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.CoreTestConstants;

/**
 * Test {@link DefinePropertyAction}.
 * 
 * @author Aleksey Didik
 */
@RunWith(RobolectricTestRunner.class)
public class DefinePropertyActionTest {

  private static final String DEFINE_INPUT_DIR = CoreTestConstants.JORAN_INPUT_PREFIX
      + "define/";
  private static final String GOOD_XML = "good.xml";
  private static final String NONAME_XML = "noname.xml";
  private static final String NOCLASS_XML = "noclass.xml";
  private static final String BADCLASS_XML = "badclass.xml";

  SimpleConfigurator simpleConfigurator;
  Context context = new ContextBase();
  DefinePropertyAction definerAction;
  InterpretationContext ic;
  StatusChecker checker = new StatusChecker(context);

  @Before
  public void setUp() throws Exception {

    HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();
    rulesMap.put(new ElementSelector("define"), new DefinePropertyAction());
    simpleConfigurator = new SimpleConfigurator(rulesMap);
    simpleConfigurator.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    //StatusPrinter.printInCaseOfErrorsOrWarnings(context);
  }

  @Test
  public void good() throws JoranException {
    simpleConfigurator.doConfigure(DEFINE_INPUT_DIR + GOOD_XML);
    InterpretationContext ic = simpleConfigurator.getInterpreter().getInterpretationContext();
    String inContextFoo = ic.getProperty("foo");
    assertEquals("monster", inContextFoo);
  }

  @Test
  public void noName() throws JoranException {
    simpleConfigurator.doConfigure(DEFINE_INPUT_DIR + NONAME_XML);
    // get from context
    String inContextFoo = context.getProperty("foo");
    assertNull(inContextFoo);
    // check context errors
    checker.assertContainsMatch(Status.ERROR,
        "Missing property name for property definer. Near \\[define\\] line 1");
  }

  @Test
  public void noClass() throws JoranException {
    simpleConfigurator.doConfigure(DEFINE_INPUT_DIR + NOCLASS_XML);
    String inContextFoo = context.getProperty("foo");
    assertNull(inContextFoo);
    checker.assertContainsMatch(Status.ERROR,
        "Missing class name for property definer. Near \\[define\\] line 1");
  }

  @Test
  public void testBadClass() throws JoranException {
    simpleConfigurator.doConfigure(DEFINE_INPUT_DIR + BADCLASS_XML);
    // get from context
    String inContextFoo = context.getProperty("foo");
    assertNull(inContextFoo);
    // check context errors
    checker.assertContainsMatch(Status.ERROR, "Could not create an PropertyDefiner of type");
  }

}
