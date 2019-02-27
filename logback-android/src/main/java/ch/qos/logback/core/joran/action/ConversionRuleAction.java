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
import java.util.Map;


import org.xml.sax.Attributes;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;



public class ConversionRuleAction extends Action {
  boolean inError = false;
  
  /**
   * Instantiates an layout of the given class and sets its name.
   *
   */
  @SuppressWarnings("unchecked")
  public void begin(InterpretationContext ec, String localName, Attributes attributes) {
    // Let us forget about previous errors (in this object)
    inError = false;

    String errorMsg;
    String conversionWord =
      attributes.getValue(ActionConst.CONVERSION_WORD_ATTRIBUTE);
    String converterClass =
      attributes.getValue(ActionConst.CONVERTER_CLASS_ATTRIBUTE);

    if (OptionHelper.isEmpty(conversionWord)) {
      inError = true;
      errorMsg = "No 'conversionWord' attribute in <conversionRule>";
      addError(errorMsg);

      return;
    }

    if (OptionHelper.isEmpty(converterClass)) {
      inError = true;
      errorMsg = "No 'converterClass' attribute in <conversionRule>";
      ec.addError(errorMsg);

      return;
    }

    try {
      Map<String, String> ruleRegistry = (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
      if(ruleRegistry == null) {
        ruleRegistry = new HashMap<String, String>();
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
      }
      // put the new rule into the rule registry
      addInfo("registering conversion word "+conversionWord+" with class ["+converterClass+"]");
      ruleRegistry.put(conversionWord, converterClass);
    } catch (Exception oops) {
      inError = true;
      errorMsg = "Could not add conversion rule to PatternLayout.";
      addError(errorMsg);
    }
  }

  /**
   * Once the children elements are also parsed, now is the time to activate
   * the appender options.
   */
  public void end(InterpretationContext ec, String n) {
  }

  public void finish(InterpretationContext ec) {
  }
}
