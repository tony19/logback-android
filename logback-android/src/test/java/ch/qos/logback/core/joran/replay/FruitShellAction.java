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

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;

/** 
 * The Fruit* code is intended to test Joran's replay capability
 * */
public class FruitShellAction extends Action {

  FruitShell fruitShell;
  private boolean inError = false;

  
  @Override
  public void begin(InterpretationContext ec, String name, Attributes attributes)
      throws ActionException {

    // We are just beginning, reset variables
    fruitShell = new FruitShell();
    inError = false;
    
    try {


      fruitShell.setContext(context);

      String shellName = attributes.getValue(NAME_ATTRIBUTE);

      if (OptionHelper.isEmpty(shellName)) {
        addWarn(
          "No appender name given for fruitShell].");
      } else {
        fruitShell.setName(shellName);
        addInfo("FruitShell named as [" + shellName + "]");
      }

      ec.pushObject(fruitShell);
    } catch (Exception oops) {
      inError = true;
      addError(
        "Could not create an FruitShell", oops);
      throw new ActionException(oops);
    }
  }

  @Override
  public void end(InterpretationContext ec, String name) throws ActionException {
    if (inError) {
      return;
    }

    Object o = ec.peekObject();

    if (o != fruitShell) {
      addWarn(
        "The object at the of the stack is not the fruitShell named ["
        + fruitShell.getName() + "] pushed earlier.");
    } else {
      addInfo(
        "Popping fruitSHell named [" + fruitShell.getName()
        + "] from the object stack");
      ec.popObject();
      FruitContext fruitContext = (FruitContext) ec.getContext();
      fruitContext.addFruitShell(fruitShell);
    }
  }

  
}
