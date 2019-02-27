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

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.InterpretationContext;

public class ContextNameAction extends Action {

  public void begin(InterpretationContext ec, String name, Attributes attributes) {
  }

  public void body(InterpretationContext ec, String body) {

    String finalBody = ec.subst(body);
    addInfo("Setting logger context name as [" + finalBody + "]");
    try {
      context.setName(finalBody);
    } catch (IllegalStateException e) {
      addError("Failed to rename context [" + context.getName() + "] as ["
          + finalBody + "]", e);
    }
  }

  public void end(InterpretationContext ec, String name) {
  }
}
