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

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;

public class FruitContextAction extends Action {

  private boolean inError = false;

  
  @Override
  public void begin(InterpretationContext ec, String name, Attributes attributes)
      throws ActionException {

    inError = false;
    
    try {
      ec.pushObject(context);
    } catch (Exception oops) {
      inError = true;
      addError(
        "Could not push context", oops);
      throw new ActionException(oops);
    }
  }

  @Override
  public void end(InterpretationContext ec, String name) throws ActionException {
    if (inError) {
      return;
    }

    Object o = ec.peekObject();

    if (o != context) {
      addWarn(
        "The object at the of the stack is not the context named ["
        + context.getName() + "] pushed earlier.");
    } else {
      addInfo(
        "Popping context named [" + context.getName()
        + "] from the object stack");
      ec.popObject();
    }
  }

  
}
