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
import ch.qos.logback.core.joran.util.PropertySetter;



public class ParamAction extends Action {
  static String NO_NAME = "No "+NAME_ATTRIBUTE+" attribute in <param> element";
  static String NO_VALUE = "No "+VALUE_ATTRIBUTE+" attribute in <param> element";
  boolean inError = false;

  public void begin(
    InterpretationContext ec, String localName, Attributes attributes) {
    String name = attributes.getValue(NAME_ATTRIBUTE);
    String value = attributes.getValue(VALUE_ATTRIBUTE);

    if (name == null) {
      inError = true;
      addError(NO_NAME);
      return;
    }

    if (value == null) {
      inError = true;
      addError(NO_VALUE);
      return;
    }

    // remove both leading and trailing spaces
    value = value.trim();

    Object o = ec.peekObject();
    PropertySetter propSetter = new PropertySetter(o);
    propSetter.setContext(context);
    value = ec.subst(value);

    // allow for variable substitution for name as well
    name = ec.subst(name);

    //getLogger().debug(
    //  "In ParamAction setting parameter [{}] to value [{}].", name, value);
    propSetter.setProperty(name, value);
  }

  public void end(InterpretationContext ec, String localName) {
  }

  public void finish(InterpretationContext ec) {
  }
}
