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

import ch.qos.logback.core.util.CachingDateFormatter;
import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Given a key and a date-and-time pattern, puts a property to the context, with
 * the specified key and value equal to the current time in the format
 * corresponding to the specified date-and-time pattern.
 *
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public class TimestampAction extends Action {
  static String DATE_PATTERN_ATTRIBUTE = "datePattern";
  static String TIME_REFERENCE_ATTRIBUTE = "timeReference";
  static String CONTEXT_BIRTH = "contextBirth";

  boolean inError = false;

  @Override
  public void begin(InterpretationContext ec, String name, Attributes attributes)
      throws ActionException {
    String keyStr = attributes.getValue(KEY_ATTRIBUTE);
    if (OptionHelper.isEmpty(keyStr)) {
      addError("Attribute named [" + KEY_ATTRIBUTE + "] cannot be empty");
      inError = true;
    }
    String datePatternStr = attributes.getValue(DATE_PATTERN_ATTRIBUTE);
    if (OptionHelper.isEmpty(datePatternStr)) {
      addError("Attribute named [" + DATE_PATTERN_ATTRIBUTE
          + "] cannot be empty");
      inError = true;
    }

    String timeReferenceStr = attributes.getValue(TIME_REFERENCE_ATTRIBUTE);
    long timeReference;
    if (CONTEXT_BIRTH.equalsIgnoreCase(timeReferenceStr)) {
      addInfo("Using context birth as time reference.");
      timeReference = context.getBirthTime();
    } else {
      timeReference =  System.currentTimeMillis();
      addInfo("Using current interpretation time, i.e. now, as time reference.");
    }


    if (inError)
      return;

    String scopeStr = attributes.getValue(SCOPE_ATTRIBUTE);
    Scope scope = ActionUtil.stringToScope(scopeStr);

    CachingDateFormatter sdf = new CachingDateFormatter(datePatternStr);
    String val = sdf.format(timeReference);

    addInfo("Adding property to the context with key=\"" + keyStr
        + "\" and value=\"" + val + "\" to the " + scope + " scope");
    ActionUtil.setProperty(ec, keyStr, val, scope);
  }

  @Override
  public void end(InterpretationContext ec, String name) throws ActionException {
  }

}
