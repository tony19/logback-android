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
package ch.qos.logback.classic.joran.action;

import org.xml.sax.Attributes;

import ch.qos.logback.classic.net.ReceiverBase;
import ch.qos.logback.classic.net.SocketReceiver;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;

/**
 * A Joran {@link Action} for a {@link SocketReceiver} configuration.
 *
 * @author Carl Harris
 */
public class ReceiverAction extends Action {

  private ReceiverBase receiver;
  private boolean inError;

  @Override
  public void begin(InterpretationContext ic, String name,
      Attributes attributes) throws ActionException {

    String className = attributes.getValue(CLASS_ATTRIBUTE);
    if (OptionHelper.isEmpty(className)) {
      addError("Missing class name for receiver. Near [" + name
          + "] line " + getLineNumber(ic));
      inError = true;
      return;
    }

    try {
      addInfo("About to instantiate receiver of type [" + className + "]");

      receiver = (ReceiverBase) OptionHelper.instantiateByClassName(
          className, ReceiverBase.class, context);
      receiver.setContext(context);
      ic.pushObject(receiver);
    }
    catch (Exception ex) {
      inError = true;
      addError("Could not create a receiver of type [" + className + "].", ex);
      throw new ActionException(ex);
    }
  }

  @Override
  public void end(InterpretationContext ic, String name)
      throws ActionException {

    if (inError) return;

    ic.getContext().register(receiver);
    receiver.start();

    Object o = ic.peekObject();
    if (o != receiver) {
      addWarn("The object at the of the stack is not the remote " +
      		"pushed earlier.");
    } else {
      ic.popObject();
    }
  }

}
