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
package ch.qos.logback.core.joran.implicitAction;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.core.joran.SimpleConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.StatusPrinter;

@RunWith(RobolectricTestRunner.class)
public class ImplicitActionTest {

  static final String IMPLCIT_DIR = CoreTestConstants.TEST_DIR_PREFIX
      + "input/joran/implicitAction/";

  FruitContext fruitContext = new FruitContext();
  SimpleConfigurator simpleConfigurator;

  @Before
  public void setUp() throws Exception {
    fruitContext.setName("fruits");
    HashMap<ElementSelector, Action> rulesMap = new HashMap<ElementSelector, Action>();
    rulesMap.put(new ElementSelector("/context/"), new FruitContextAction());
    simpleConfigurator = new SimpleConfigurator(rulesMap);
    simpleConfigurator.setContext(fruitContext);
  }

  void verifyFruit() {
    List<Fruit> fList = fruitContext.getFruitList();
    assertNotNull(fList);
    assertEquals(1, fList.size());

    Fruit f0 = fList.get(0);
    assertEquals("blue", f0.getName());
    assertEquals(2, f0.textList.size());
    assertEquals("hello", f0.textList.get(0));
    assertEquals("world", f0.textList.get(1));
  }

  @Test
  public void nestedComplex() throws Exception {
    try {
      simpleConfigurator.doConfigure(IMPLCIT_DIR + "nestedComplex.xml");
      verifyFruit();

    } catch (Exception je) {
      StatusPrinter.print(fruitContext);
      throw je;
    }
  }

  @Test
  public void nestedComplexWithoutClassAtrribute() throws Exception {
    try {
      simpleConfigurator.doConfigure(IMPLCIT_DIR
          + "nestedComplexWithoutClassAtrribute.xml");

      verifyFruit();

    } catch (Exception je) {
      StatusPrinter.print(fruitContext);
      throw je;
    }
  }

  
  void verifyFruitList() {
    List<Fruit> fList = fruitContext.getFruitList();
    assertNotNull(fList);
    assertEquals(1, fList.size());

    Fruit f0 = fList.get(0);
    assertEquals(2, f0.cakeList.size());

    Cake cakeA = f0.cakeList.get(0);
    assertEquals("A", cakeA.getType());

    Cake cakeB = f0.cakeList.get(1);
    assertEquals("B", cakeB.getType());
  }
  @Test
  public void nestedComplexCollection() throws Exception {
    try {
      simpleConfigurator.doConfigure(IMPLCIT_DIR
          + "nestedComplexCollection.xml");
      verifyFruitList();
    } catch (Exception je) {
      StatusPrinter.print(fruitContext);
      throw je;
    }
  }

  
  @Test
  public void nestedComplexCollectionWithoutClassAtrribute() throws Exception {
    try {
      simpleConfigurator.doConfigure(IMPLCIT_DIR
          + "nestedComplexCollectionWithoutClassAtrribute.xml");
      verifyFruitList();
    } catch (Exception je) {
      StatusPrinter.print(fruitContext);
      throw je;
    }
  }

}
