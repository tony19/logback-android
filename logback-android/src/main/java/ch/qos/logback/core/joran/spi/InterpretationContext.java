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
package ch.qos.logback.core.joran.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.xml.sax.Locator;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.android.AndroidContextUtil;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.util.OptionHelper;

/**
 *
 * An InterpretationContext contains the contextual state of a Joran parsing
 * session. {@link Action} objects depend on this context to exchange and store
 * information.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class InterpretationContext extends ContextAwareBase implements
    PropertyContainer {
  Stack<Object> objectStack;
  Map<String, Object> objectMap;
  Map<String, String> propertiesMap;
  static boolean androidContextInitDone = false;

  Interpreter joranInterpreter;
  final List<InPlayListener> listenerList = new ArrayList<InPlayListener>();
  DefaultNestedComponentRegistry defaultNestedComponentRegistry = new DefaultNestedComponentRegistry();

  public InterpretationContext(Context context, Interpreter joranInterpreter) {
    this.context = context;
    this.joranInterpreter = joranInterpreter;
    objectStack = new Stack<Object>();
    objectMap = new HashMap<String, Object>(5);
    propertiesMap = new HashMap<String, String>(5);
  }


  public DefaultNestedComponentRegistry getDefaultNestedComponentRegistry() {
    return defaultNestedComponentRegistry;
  }

  public Map<String, String> getCopyOfPropertyMap() {
    return new HashMap<String, String>(propertiesMap);
  }

  void setPropertiesMap(Map<String, String> propertiesMap) {
    this.propertiesMap = propertiesMap;
  }

  String updateLocationInfo(String msg) {
    Locator locator = joranInterpreter.getLocator();

    if (locator != null) {
      return msg + locator.getLineNumber() + ":" + locator.getColumnNumber();
    } else {
      return msg;
    }
  }

  public Locator getLocator() {
    return joranInterpreter.getLocator();
  }

  public Interpreter getJoranInterpreter() {
    return joranInterpreter;
  }

  public Stack<Object> getObjectStack() {
    return objectStack;
  }

  public boolean isEmpty() {
    return objectStack.isEmpty();
  }

  public Object peekObject() {
    return objectStack.peek();
  }

  public void pushObject(Object o) {
    objectStack.push(o);
  }

  public Object popObject() {
    return objectStack.pop();
  }

  public Object getObject(int i) {
    return objectStack.get(i);
  }

  public Map<String, Object> getObjectMap() {
    return objectMap;
  }

  /**
   * Add a property to the properties of this execution context. If the property
   * exists already, it is overwritten.
   * @param key the property's key
   * @param value the value associated with the key
   */
  public void addSubstitutionProperty(String key, String value) {
    if (key == null || value == null) {
      return;
    }
    // values with leading or trailing spaces are bad. We remove them now.
    value = value.trim();
    propertiesMap.put(key, value);
  }

  public void addSubstitutionProperties(Properties props) {
    if (props == null) {
      return;
    }

    for(Object keyObject: props.keySet()) {
      String key = (String) keyObject;
      String val = props.getProperty(key);
      addSubstitutionProperty(key, val);
    }
  }

  /**
   * If a key is found in propertiesMap then return it. Otherwise, delegate to
   * the context.
   */
  public String getProperty(String key) {
    String v = propertiesMap.get(key);
    if (v != null) {
      return v;
    } else {
      return context.getProperty(key);
    }
  }

  public String subst(String value) {
    if (value == null) {
      return null;
    }
    initAndroidContextIfValueHasSpecialVars(value);
    return OptionHelper.substVars(value, this, context);
  }

  private void initAndroidContextIfValueHasSpecialVars(String value) {
    if (androidContextInitDone) {
      return;
    }
    if (AndroidContextUtil.containsProperties(value)) {
      new AndroidContextUtil().setupProperties(context);
      androidContextInitDone = true;
    }
  }

  public boolean isListenerListEmpty() {
    return listenerList.isEmpty();
  }

  public void addInPlayListener(InPlayListener ipl) {
    if (listenerList.contains(ipl)) {
      addWarn("InPlayListener " + ipl + " has been already registered");
    } else {
      listenerList.add(ipl);
    }
  }

  public boolean removeInPlayListener(InPlayListener ipl) {
    return listenerList.remove(ipl);
  }

  void fireInPlay(SaxEvent event) {
    for (InPlayListener ipl : listenerList) {
      ipl.inPlay(event);
    }
  }
}
