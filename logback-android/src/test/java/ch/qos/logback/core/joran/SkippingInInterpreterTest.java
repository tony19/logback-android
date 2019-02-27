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
package ch.qos.logback.core.joran;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ch.qos.logback.core.joran.spi.ElementSelector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.action.ext.BadBeginAction;
import ch.qos.logback.core.joran.action.ext.BadEndAction;
import ch.qos.logback.core.joran.action.ext.HelloAction;
import ch.qos.logback.core.joran.action.ext.TouchAction;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.CoreTestConstants;

/**
 * Test the way Interpreter skips child elements in case of exceptions thrown by
 * Actions. It also tests addition of status messages in case of exceptions.
 * 
 * @author Ceki Gulcu
 */
@RunWith(RobolectricTestRunner.class)
public class SkippingInInterpreterTest {

  HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();
  Context context = new ContextBase();
  StatusManager sm = context.getStatusManager();

  SAXParser createParser() throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    return spf.newSAXParser();
  }

  void doTest(String filename, Integer expectedInt, Class<?> exceptionClass)
      throws Exception {

    rulesMap.put(new ElementSelector("test"), new NOPAction());
    rulesMap.put(new ElementSelector("test/badBegin"), new BadBeginAction());
    rulesMap.put(new ElementSelector("test/badBegin/touch"), new TouchAction());
    rulesMap.put(new ElementSelector("test/badEnd"), new BadEndAction());
    rulesMap.put(new ElementSelector("test/badEnd/touch"), new TouchAction());
    rulesMap.put(new ElementSelector("test/hello"), new HelloAction());

    rulesMap.put(new ElementSelector("test/isolate"), new NOPAction());
    rulesMap.put(new ElementSelector("test/isolate/badEnd"), new BadEndAction());
    rulesMap.put(new ElementSelector("test/isolate/badEnd/touch"), new TouchAction());
    rulesMap.put(new ElementSelector("test/isolate/touch"), new TouchAction());
    rulesMap.put(new ElementSelector("test/hello"), new HelloAction());

    TrivialConfigurator tc = new TrivialConfigurator(rulesMap);
    tc.setContext(context);
    tc.doConfigure(CoreTestConstants.TEST_DIR_PREFIX + "input/joran/skip/" + filename);

    String str = context.getProperty(HelloAction.PROPERTY_KEY);
    assertEquals("Hello John Doe.", str);

    Integer i = (Integer) context.getObject(TouchAction.KEY);
    if (expectedInt == null) {
      assertNull(i);
    } else {
      assertEquals(expectedInt, i);
    }

    // check the existence of an ERROR status
    List<Status> statusList = sm.getCopyOfStatusList();
    Status s0 = statusList.get(0);
    assertEquals(Status.ERROR, s0.getLevel());
    assertTrue(s0.getThrowable().getClass() == exceptionClass);
  }

  @Test
  public void testSkippingRuntimeExInBadBegin() throws Exception {
    doTest("badBegin1.xml", null, IllegalStateException.class);
  }

  @Test
  public void testSkippingActionExInBadBegin() throws Exception {
    doTest("badBegin2.xml", null, ActionException.class);
  }

  @Test
  public void testSkippingRuntimeExInBadEnd() throws Exception {
    doTest("badEnd1.xml", 2, IllegalStateException.class);
  }

  @Test
  public void testSkippingActionExInBadEnd() throws Exception {
    doTest("badEnd2.xml", 2, ActionException.class);
  }
}
