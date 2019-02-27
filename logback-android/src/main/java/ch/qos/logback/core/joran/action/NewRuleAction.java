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

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.util.OptionHelper;


public class NewRuleAction extends Action {
  boolean inError = false;

  /**
   * Instantiates an layout of the given class and sets its name.
   */
  public void begin(InterpretationContext ec, String localName, Attributes attributes) {
    // Let us forget about previous errors (in this object)
    inError = false;
    String errorMsg;
    String pattern = attributes.getValue(Action.PATTERN_ATTRIBUTE);
    String actionClass = attributes.getValue(Action.ACTION_CLASS_ATTRIBUTE);

    if (OptionHelper.isEmpty(pattern)) {
      inError = true;
      errorMsg = "No 'pattern' attribute in <newRule>";
      addError(errorMsg);
      return;
    }

    if (OptionHelper.isEmpty(actionClass)) {
      inError = true;
      errorMsg = "No 'actionClass' attribute in <newRule>";
      addError(errorMsg);
      return;
    }

    try {
      addInfo("About to add new Joran parsing rule [" + pattern + ","
          + actionClass + "].");
      ec.getJoranInterpreter().getRuleStore().addRule(new ElementSelector(pattern),
          actionClass);
    } catch (Exception oops) {
      inError = true;
      errorMsg = "Could not add new Joran parsing rule [" + pattern + ","
          + actionClass + "]";
      addError(errorMsg);
    }
  }

  /**
   * Once the children elements are also parsed, now is the time to activate the
   * appender options.
   */
  public void end(InterpretationContext ec, String n) {
  }

  public void finish(InterpretationContext ec) {
  }
}
