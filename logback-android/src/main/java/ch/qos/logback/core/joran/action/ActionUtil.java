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

import java.util.Properties;

import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.ContextUtil;
import ch.qos.logback.core.util.OptionHelper;

public class ActionUtil {

  public enum Scope {
    LOCAL, CONTEXT, SYSTEM
  };

  /**
   * Convert a string into a scope. Scope.LOCAL is returned by default.
   * @param scopeStr the string to be converted
   * @return a scope corresponding to the input string;  Scope.LOCAL by default.
   */
  static public Scope stringToScope(String scopeStr) {
    if(Scope.SYSTEM.toString().equalsIgnoreCase(scopeStr))
      return Scope.SYSTEM;
     if(Scope.CONTEXT.toString().equalsIgnoreCase(scopeStr))
      return Scope.CONTEXT;

    return Scope.LOCAL;
  }

  static public void setProperty(InterpretationContext ic, String key, String value, Scope scope) {
    switch (scope) {
    case LOCAL:
      ic.addSubstitutionProperty(key, value);
      break;
    case CONTEXT:
      ic.getContext().putProperty(key, value);
      break;
    case SYSTEM:
      OptionHelper.setSystemProperty(ic, key, value);
    }
  }

  /**
   * Add all the properties found in the argument named 'props' to an
   * InterpretationContext.
   *
   * @param ic interpretation context
   * @param props the properties to set in the context
   * @param scope scope of properties
   */
  static public void setProperties(InterpretationContext ic, Properties props,
      Scope scope) {
    switch (scope) {
    case LOCAL:
      ic.addSubstitutionProperties(props);
      break;
    case CONTEXT:
      ContextUtil cu = new ContextUtil(ic.getContext());
      cu.addProperties(props);
      break;
    case SYSTEM:
      OptionHelper.setSystemProperties(ic, props);
    }
  }

}
