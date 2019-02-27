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
package ch.qos.logback.core.joran.replay;

import ch.qos.logback.core.spi.ContextAwareBase;

public class FruitShell extends ContextAwareBase {

  FruitFactory fruitFactory;
  String name;
  
  public void setFruitFactory(FruitFactory fruitFactory) {
    this.fruitFactory = fruitFactory;
  }

  void testFruit() {
    
    Fruit fruit = fruitFactory.buildFruit();
    System.out.println(fruit);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Constructs a <code>String</code> with all attributes
   * in name = value format.
   *
   * @return a <code>String</code> representation 
   * of this object.
   */
  public String toString()
  {
      final String TAB = " ";
      
      String retValue = "";
      
      retValue = "FruitShell ( "
          + "fruitFactory = " + this.fruitFactory + TAB
          + "name = " + this.name + TAB
          + " )";
      
      return retValue;
  }
  
}
