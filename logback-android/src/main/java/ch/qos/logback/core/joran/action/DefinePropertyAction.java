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

import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.spi.PropertyDefiner;
import org.xml.sax.Attributes;

/**
 * Instantiate class for define property value. Get future property name and
 * property definer class from attributes. Some property definer properties
 * could be used. After defining put new property to context.
 * 
 * @author Aleksey Didik
 */
public class DefinePropertyAction extends Action {

  String scopeStr;
  Scope scope;
  String propertyName;
  PropertyDefiner definer;
  boolean inError;

  public void begin(InterpretationContext ec, String localName,
      Attributes attributes) throws ActionException {
    // reset variables
    scopeStr = null;
    scope = null;
    propertyName = null;
    definer = null;
    inError = false;
    
    // read future property name
    propertyName = attributes.getValue(NAME_ATTRIBUTE);
    scopeStr = attributes.getValue(SCOPE_ATTRIBUTE);
    
    scope = ActionUtil.stringToScope(scopeStr);
    if (OptionHelper.isEmpty(propertyName)) {
      addError("Missing property name for property definer. Near [" + localName
          + "] line " + getLineNumber(ec));
      inError = true;
      return;
    }

    // read property definer class name
    String className = attributes.getValue(CLASS_ATTRIBUTE);
    if (OptionHelper.isEmpty(className)) {
      addError("Missing class name for property definer. Near [" + localName
          + "] line " + getLineNumber(ec));
      inError = true;
      return;
    }

    // try to instantiate property definer
    try {
      addInfo("About to instantiate property definer of type [" + className
          + "]");
      definer = (PropertyDefiner) OptionHelper.instantiateByClassName(
          className, PropertyDefiner.class, context);
      definer.setContext(context);
      if(definer instanceof LifeCycle) {
        ((LifeCycle) definer).start();
      }
      ec.pushObject(definer);
    } catch (Exception oops) {
      inError = true;
      addError("Could not create an PropertyDefiner of type [" + className
          + "].", oops);
      throw new ActionException(oops);
    }
  }

  /**
   * Now property definer is initialized by all properties and we can put
   * property value to context
   */
  public void end(InterpretationContext ec, String name) {
    if (inError) {
      return;
    }

    Object o = ec.peekObject();

    if (o != definer) {
      addWarn("The object at the of the stack is not the property definer for property named ["
          + propertyName + "] pushed earlier.");
    } else {
      addInfo("Popping property definer for property named [" + propertyName
          + "] from the object stack");
      ec.popObject();
      // let's put defined property and value to context but only if it is
      // not null
      String propertyValue = definer.getPropertyValue();
      if(propertyValue != null) {
        ActionUtil.setProperty(ec, propertyName, propertyValue, scope);
      }
    }
  }
}
