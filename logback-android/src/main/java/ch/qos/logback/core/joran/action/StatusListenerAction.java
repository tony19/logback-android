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

import ch.qos.logback.core.spi.ContextAware;
import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.util.OptionHelper;


public class StatusListenerAction extends Action {


  boolean inError = false;
  Boolean effectivelyAdded = null;
  StatusListener statusListener = null;

  public void begin(InterpretationContext ec, String name, Attributes attributes) throws ActionException {
    inError = false;
    effectivelyAdded = null;
    String className = attributes.getValue(CLASS_ATTRIBUTE);
    if (OptionHelper.isEmpty(className)) {
      addError("Missing class name for statusListener. Near ["
              + name + "] line " + getLineNumber(ec));
      inError = true;
      return;
    }

    try {
      statusListener = (StatusListener) OptionHelper.instantiateByClassName(
              className, StatusListener.class, context);
      effectivelyAdded = ec.getContext().getStatusManager().add(statusListener);
      if (statusListener instanceof ContextAware) {
        ((ContextAware) statusListener).setContext(context);
      }
      addInfo("Added status listener of type [" + className + "]");
      ec.pushObject(statusListener);
    } catch (Exception e) {
      inError = true;
      addError(
              "Could not create an StatusListener of type [" + className + "].", e);
      throw new ActionException(e);
    }

  }

  public void finish(InterpretationContext ec) {
  }

  public void end(InterpretationContext ec, String e) {
    if (inError) {
      return;
    }
    if (isEffectivelyAdded() && statusListener instanceof LifeCycle) {
      ((LifeCycle) statusListener).start();
    }
    Object o = ec.peekObject();
    if (o != statusListener) {
      addWarn("The object at the of the stack is not the statusListener pushed earlier.");
    } else {
      ec.popObject();
    }
  }

  private boolean isEffectivelyAdded() {
    if (effectivelyAdded == null) {
      return false;
    }
    return effectivelyAdded;
  }
}
