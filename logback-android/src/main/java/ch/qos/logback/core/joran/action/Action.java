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
import org.xml.sax.Locator;

import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 *
 * Most of the work for configuring logback is done by Actions.
 *
 * <p>Action methods are invoked as the XML file is parsed.
 *
 * <p>This class is largely inspired from the relevant class in the
 * commons-digester project of the Apache Software Foundation.
 *
 * @author Craig McClanahan
 * @author Christopher Lenz
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public abstract class Action extends ContextAwareBase {

  public static final String NAME_ATTRIBUTE = "name";
  public static final String KEY_ATTRIBUTE = "key";
  public static final String VALUE_ATTRIBUTE = "value";
  public static final String FILE_ATTRIBUTE = "file";
  public static final String CLASS_ATTRIBUTE = "class";
  public static final String PATTERN_ATTRIBUTE = "pattern";
  public static final String SCOPE_ATTRIBUTE = "scope";


  public static final String ACTION_CLASS_ATTRIBUTE = "actionClass";

  /**
   * Called when the parser encounters an element matching a
   * {@link ch.qos.logback.core.joran.spi.ElementSelector Pattern}.
   *
   * @param ic interpretation context
   * @param name name of action
   * @param attributes attributes of action
   * @throws ActionException failed to process action
   */
  public abstract void begin(InterpretationContext ic, String name,
      Attributes attributes) throws ActionException;

  /**
   * Called to pass the body (as text) contained within an element.
   * @param ic interpretation context
   * @param body text of the element
   * @throws ActionException the body could not be parsed
   */
  public void body(InterpretationContext ic, String body)
      throws ActionException {
    // NOP
  }

  /*
   * Called when the parser encounters an endElement event matching a
   * {@link ch.qos.logback.core.joran.spi.Pattern Pattern}.
   */
  public abstract void end(InterpretationContext ic, String name)
      throws ActionException;

  public String toString() {
    return this.getClass().getName();
  }

  protected int getColumnNumber(InterpretationContext ic) {
    Interpreter ji = ic.getJoranInterpreter();
    Locator locator = ji.getLocator();
    if (locator != null) {
      return locator.getColumnNumber();
    }
    return -1;
  }

  protected int getLineNumber(InterpretationContext ic) {
    Interpreter ji = ic.getJoranInterpreter();
    Locator locator = ji.getLocator();
    if (locator != null) {
      return locator.getLineNumber();
    }
    return -1;
  }

  protected String getLineColStr(InterpretationContext ic) {
    return "line: " + getLineNumber(ic) + ", column: "
        + getColumnNumber(ic);
  }
}
