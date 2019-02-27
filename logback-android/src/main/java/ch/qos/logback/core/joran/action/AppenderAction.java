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
package ch.qos.logback.core.joran.action;

import java.util.HashMap;

import org.xml.sax.Attributes;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;

public class AppenderAction<E> extends Action {
  Appender<E> appender;
  private boolean inError = false;

  /**
   * Instantiates an appender of the given class and sets its name.
   *
   * The appender thus generated is placed in the {@link InterpretationContext}'s
   * appender bag.
   */
  @SuppressWarnings("unchecked")
  public void begin(InterpretationContext ec, String localName,
      Attributes attributes) throws ActionException {
    // We are just beginning, reset variables
    appender = null;
    inError = false;

    String className = attributes.getValue(CLASS_ATTRIBUTE);
    if (OptionHelper.isEmpty(className)) {
      addError("Missing class name for appender. Near [" + localName
          + "] line " + getLineNumber(ec));
      inError = true;
      return;
    }

    try {
      addInfo("About to instantiate appender of type [" + className + "]");
      warnDeprecated(className);

      appender = (Appender<E>) OptionHelper.instantiateByClassName(className,
          ch.qos.logback.core.Appender.class, context);

      appender.setContext(context);

      String appenderName = ec.subst(attributes.getValue(NAME_ATTRIBUTE));

      if (OptionHelper.isEmpty(appenderName)) {
        addWarn("No appender name given for appender of type " + className
            + "].");
      } else {
        appender.setName(appenderName);
        addInfo("Naming appender as [" + appenderName + "]");
      }

      // The execution context contains a bag which contains the appenders
      // created thus far.
      HashMap<String, Appender<E>> appenderBag = (HashMap<String, Appender<E>>) ec.getObjectMap().get(
          ActionConst.APPENDER_BAG);

      // add the appender just created to the appender bag.
      appenderBag.put(appenderName, appender);

      ec.pushObject(appender);
    } catch (Exception oops) {
      inError = true;
      addError("Could not create an Appender of type [" + className + "].",
          oops);
      throw new ActionException(oops);
    }
  }

  private void warnDeprecated(String className) {
    if (className.equals("ch.qos.logback.core.ConsoleAppender")) {
      addWarn("ConsoleAppender is deprecated for LogcatAppender");
    }
  }

  /**
   * Once the children elements are also parsed, now is the time to activate the
   * appender options.
   */
  public void end(InterpretationContext ec, String name) {
    if (inError) {
      return;
    }

    if (appender instanceof LifeCycle) {
      ((LifeCycle) appender).start();
    }

    Object o = ec.peekObject();

    if (o != appender) {
      addWarn("The object at the of the stack is not the appender named ["
          + appender.getName() + "] pushed earlier.");
    } else {
      ec.popObject();
    }
  }
}
