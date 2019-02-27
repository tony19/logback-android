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


import ch.qos.logback.core.joran.spi.ElementPath;
import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.spi.InterpretationContext;


/**
 * ImplcitActions are like normal (explicit) actions except that are applied
 * by the parser when no other pattern applies. Since there can be many implicit
 * actions, each action is asked whether it applies in the given context. The
 * first implicit action to respond positively is then applied. See also the
 * {@link #isApplicable} method.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public abstract class ImplicitAction extends Action {

  /**
   * Check whether this implicit action is appropriate in the current context.
   *
   * @param currentElementPath This pattern contains the tag name of the current
   * element being parsed at the top of the stack.
   * @param attributes The attributes of the current element to process.
   * @param ec interpretation context
   * @return Whether the implicit action is applicable in the current context
   */
  public abstract boolean isApplicable(
    ElementPath currentElementPath, Attributes attributes, InterpretationContext ec);


}
