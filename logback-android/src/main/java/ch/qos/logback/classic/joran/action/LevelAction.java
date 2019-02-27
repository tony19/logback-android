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
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.InterpretationContext;

/**
 * Action to handle the &lt;level&gt; element nested within &lt;logger&gt; element.
 *
 * <p>This action is <b>deprecated</b>. Use the level attribute within the logger
 * element.
 *
 * @deprecated
 * @author Ceki Gulcu
 */
@Deprecated
public class LevelAction extends Action {

  boolean inError = false;

  public void begin(InterpretationContext ec, String name, Attributes attributes) {
    Object o = ec.peekObject();

    if (!(o instanceof Logger)) {
      inError = true;
      addError("For element <level>, could not find a logger at the top of execution stack.");
      return;
    }

    Logger l = (Logger) o;

    String loggerName = l.getName();

    String levelStr = ec.subst(attributes.getValue(ActionConst.VALUE_ATTR));
    //addInfo("Encapsulating logger name is [" + loggerName
    //    + "], level value is  [" + levelStr + "].");

    if (ActionConst.INHERITED.equalsIgnoreCase(levelStr) || ActionConst.NULL.equalsIgnoreCase(levelStr)) {
      l.setLevel(null);
    } else {
      l.setLevel(Level.toLevel(levelStr, Level.DEBUG));
    }

    addInfo(loggerName + " level set to " + l.getLevel());
  }

  public void finish(InterpretationContext ec) {
  }

  public void end(InterpretationContext ec, String e) {
  }
}
