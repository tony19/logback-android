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

import ch.qos.logback.core.joran.GenericConfigurator;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.action.NestedComplexPropertyIA;
import ch.qos.logback.core.joran.action.NestedBasicPropertyIA;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.EventPlayer;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;

public class FruitConfigurator extends GenericConfigurator {

  FruitFactory ff;
  public FruitConfigurator(FruitFactory ff) {
    this.ff = ff;
  }

  @Override
  final public void doConfigure(final List<SaxEvent> eventList)
      throws JoranException {
    buildInterpreter();
    interpreter.getInterpretationContext().pushObject(ff);
    EventPlayer player = new EventPlayer(interpreter);
    player.play(eventList);
  }

  @Override
  protected void addImplicitRules(Interpreter interpreter) {
    NestedComplexPropertyIA nestedIA = new NestedComplexPropertyIA();
    nestedIA.setContext(context);
    interpreter.addImplicitAction(nestedIA);
    
    NestedBasicPropertyIA nestedSimpleIA = new NestedBasicPropertyIA();
    nestedIA.setContext(context);
    interpreter.addImplicitAction(nestedSimpleIA);
  }

  
  @Override
  protected void addInstanceRules(RuleStore rs) {
    rs.addRule(new ElementSelector("fruitShell"), new NOPAction());
  }

}
