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
package ch.qos.logback.core.joran.event;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.TrivialConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.CoreTestConstants;

@RunWith(RobolectricTestRunner.class)
public class InPlayFireTest  {

  Context context = new ContextBase();
  HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();

  @Test
  public void testBasic() throws JoranException {
    ListenAction listenAction = new ListenAction();
    
    rulesMap.put(new ElementSelector("fire"), listenAction);
    TrivialConfigurator gc = new TrivialConfigurator(rulesMap);

    gc.setContext(context);
    gc.doConfigure(CoreTestConstants.TEST_DIR_PREFIX + "input/joran/fire1.xml");
    
    //for(SaxEvent se: listenAction.getSeList()) {
    //  System.out.println(se);
    //}
    assertEquals(5, listenAction.getSeList().size());
    assertTrue(listenAction.getSeList().get(0) instanceof StartEvent);
    assertTrue(listenAction.getSeList().get(1) instanceof StartEvent);
    assertTrue(listenAction.getSeList().get(2) instanceof BodyEvent);
    assertTrue(listenAction.getSeList().get(3) instanceof EndEvent);
  }

  @Test
  public void testReplay() throws JoranException {
    ListenAction listenAction = new ListenAction();
    
    rulesMap.put(new ElementSelector("fire"), listenAction);
    TrivialConfigurator gc = new TrivialConfigurator(rulesMap);

    gc.setContext(context);
    gc.doConfigure(CoreTestConstants.TEST_DIR_PREFIX + "input/joran/fire1.xml");
    
//    for(SaxEvent se: listenAction.getSeList()) {
//      System.out.println(se);
//    }
    assertEquals(5, listenAction.getSeList().size());
    assertTrue(listenAction.getSeList().get(0) instanceof StartEvent);
    assertTrue(listenAction.getSeList().get(1) instanceof StartEvent);
    assertTrue(listenAction.getSeList().get(2) instanceof BodyEvent);
    assertTrue(listenAction.getSeList().get(3) instanceof EndEvent);
  }
  
  
  
}
