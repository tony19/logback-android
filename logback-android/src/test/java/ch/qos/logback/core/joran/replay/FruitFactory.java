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

import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.JoranException;

public class FruitFactory {

  static int count = 0;
  
  private List<SaxEvent> eventList;
  Fruit fruit;
  
  public void setFruit(Fruit fruit) {
    this.fruit = fruit;
  }

  public Fruit buildFruit() {
    
    Context context = new ContextBase();
    this.fruit = null;
    context.putProperty("fruitKey", "orange-"+count);
    // for next round
    count++;
    FruitConfigurator fruitConfigurator = new FruitConfigurator(this);
    fruitConfigurator.setContext(context);
    try {
      fruitConfigurator.doConfigure(eventList);
    } catch(JoranException je) {
      je.printStackTrace();
    }
    return fruit;
  }

  public String toString() {
    final String TAB = " ";

    StringBuilder retValue = new StringBuilder();

    retValue.append("FruitFactory ( ");
    if (eventList != null && eventList.size() > 0) {
      retValue.append("event1 = ").append(eventList.get(0)).append(TAB);
    }
    retValue.append(" )");

    return retValue.toString();
  }

  public void setEventList(List<SaxEvent> eventList) {
    this.eventList = eventList;
  }

}
