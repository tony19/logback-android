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

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.util.OptionHelper;


import java.util.HashMap;

public class AppenderRefAction<E> extends Action {
  boolean inError = false;

  @SuppressWarnings("unchecked")
  public void begin(InterpretationContext ec, String tagName, Attributes attributes) {
    // Let us forget about previous errors (in this object)
    inError = false;

    // logger.debug("begin called");

    Object o = ec.peekObject();

    if (!(o instanceof AppenderAttachable)) {
      String errMsg = "Could not find an AppenderAttachable at the top of execution stack. Near ["
          + tagName + "] line " + getLineNumber(ec);
      inError = true;
      addError(errMsg);
      return;
    }

    AppenderAttachable<E> appenderAttachable = (AppenderAttachable<E>) o;

    String appenderName = ec.subst(attributes.getValue(ActionConst.REF_ATTRIBUTE));

    if (OptionHelper.isEmpty(appenderName)) {
      // print a meaningful error message and return
      String errMsg = "Missing appender ref attribute in <appender-ref> tag.";
      inError = true;
      addError(errMsg);

      return;
    }

    HashMap<String, Appender<E>> appenderBag = (HashMap<String, Appender<E>>) ec.getObjectMap().get(
        ActionConst.APPENDER_BAG);
    Appender<E> appender = (Appender<E>) appenderBag.get(appenderName);

    if (appender == null) {
      String msg = "Could not find an appender named [" + appenderName
          + "]. Did you define it below instead of above in the configuration file?";
      inError = true;
      addError(msg);
      addError("See " + CoreConstants.CODES_URL
          + "#appender_order for more details.");
      return;
    }

    addInfo("Attaching appender named [" + appenderName + "] to "
        + appenderAttachable);
    appenderAttachable.addAppender(appender);
  }

  public void end(InterpretationContext ec, String n) {
  }

}
