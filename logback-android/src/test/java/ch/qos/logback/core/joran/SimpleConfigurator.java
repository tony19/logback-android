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

import java.util.HashMap;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.NestedComplexPropertyIA;
import ch.qos.logback.core.joran.action.NestedBasicPropertyIA;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;

public class SimpleConfigurator extends GenericConfigurator {

  HashMap<ElementSelector, Action> rulesMap;
  
  public SimpleConfigurator(HashMap<ElementSelector, Action> rules) {
    this.rulesMap = rules;
  }
  
  @Override
  protected void addImplicitRules(Interpreter interpreter) {
    NestedComplexPropertyIA nestedIA = new NestedComplexPropertyIA();
    nestedIA.setContext(context);
    interpreter.addImplicitAction(nestedIA);

    NestedBasicPropertyIA nestedSimpleIA = new NestedBasicPropertyIA();
    nestedSimpleIA.setContext(context);
    interpreter.addImplicitAction(nestedSimpleIA);
  }

  public Interpreter getInterpreter() {
    return interpreter;
  }

  @Override
  protected void addInstanceRules(RuleStore rs) {
    for(ElementSelector elementSelector : rulesMap.keySet()) {
      Action action = rulesMap.get(elementSelector);
      rs.addRule(elementSelector, action);
    }
  }

}
