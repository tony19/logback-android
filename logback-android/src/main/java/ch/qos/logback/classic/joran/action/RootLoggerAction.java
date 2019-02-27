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

public class RootLoggerAction extends Action {
 
  Logger root;
  boolean inError = false;

  public void begin(InterpretationContext ec, String name, Attributes attributes) {
    inError = false;

    LoggerContext loggerContext = (LoggerContext) this.context;
    root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    String levelStr =  ec.subst(attributes.getValue(ActionConst.LEVEL_ATTRIBUTE));
    if (!OptionHelper.isEmpty(levelStr)) {
      Level level = Level.toLevel(levelStr);
      addInfo("Setting level of ROOT logger to " + level);
      root.setLevel(level);
    }
    ec.pushObject(root);
  }

  public void end(InterpretationContext ec, String name) {
    if (inError) {
      return;
    }
    Object o = ec.peekObject();
    if (o != root) {
      addWarn("The object on the top the of the stack is not the root logger");
      addWarn("It is: " + o);
    } else {
      ec.popObject();
    }
  }

  public void finish(InterpretationContext ec) {
  }
}
