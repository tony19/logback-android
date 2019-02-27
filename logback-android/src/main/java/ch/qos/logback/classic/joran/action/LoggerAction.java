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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Action which handles &lt;logger&gt; elements in configuration files.
 *
 * @author Ceki Gulcu
 */
public class LoggerAction extends Action {
  public static final String LEVEL_ATTRIBUTE = "level";

  boolean inError = false;
  Logger logger;
  public void begin(InterpretationContext ec, String name, Attributes attributes) {
    // Let us forget about previous errors (in this object)
    inError = false;
    logger = null;

    LoggerContext loggerContext = (LoggerContext) this.context;

    String loggerName = ec.subst(attributes.getValue(NAME_ATTRIBUTE));

    if (OptionHelper.isEmpty(loggerName)) {
      inError = true;
      String aroundLine = getLineColStr(ec);
      String errorMsg = "No 'name' attribute in element " + name + ", around " +aroundLine;
      addError(errorMsg);
      return;
    }

    logger = loggerContext.getLogger(loggerName);

    String levelStr =  ec.subst(attributes.getValue(LEVEL_ATTRIBUTE));

    if (!OptionHelper.isEmpty(levelStr)) {
      if (ActionConst.INHERITED.equalsIgnoreCase(levelStr)
          || ActionConst.NULL.equalsIgnoreCase(levelStr)) {
        addInfo("Setting level of logger [" + loggerName
            + "] to null, i.e. INHERITED");
        logger.setLevel(null);
      } else {
        Level level = Level.toLevel(levelStr);
        addInfo("Setting level of logger [" + loggerName + "] to " + level);
        logger.setLevel(level);
      }
    }

    String additivityStr =  ec.subst(attributes.getValue(ActionConst.ADDITIVITY_ATTRIBUTE));
    if (!OptionHelper.isEmpty(additivityStr)) {
      boolean additive = Boolean.valueOf(additivityStr);
      addInfo("Setting additivity of logger [" + loggerName + "] to "
          + additive);
      logger.setAdditive(additive);
    }
    ec.pushObject(logger);
  }

  public void end(InterpretationContext ec, String e) {
    if (inError) {
      return;
    }
    Object o = ec.peekObject();
    if (o != logger) {
      addWarn("The object on the top the of the stack is not "+logger+" pushed earlier");
      addWarn("It is: " + o);
    } else {
      ec.popObject();
    }
  }

  public void finish(InterpretationContext ec) {
  }
}
