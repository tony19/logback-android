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

import java.util.List;

import ch.qos.logback.core.joran.action.Action;

/**
 *
 * As its name indicates, a RuleStore contains 2-tuples consists of a ElementSelector
 * and an Action.
 *
 * <p>As a joran configurator goes through the elements in a document, it asks
 * the rule store whether there are rules matching the current pattern by
 * invoking the {@link #matchActions(ElementPath)} method.
 *
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public interface RuleStore {

  /**
   * Add a new rule, given by a pattern and a action class (String).
   *
   * @param elementSelector the element selector
   * @param actionClassStr class name of action to execute on element
   * @throws ClassNotFoundException the specified class name was not found
   */
  void addRule(ElementSelector elementSelector, String actionClassStr)
      throws ClassNotFoundException;

  /**
   * Add a new rule, given by a pattern and an action instance.
   *
   * @param elementSelector the element selector
   * @param action action to execute on element
   */
  void addRule(ElementSelector elementSelector, Action action);

  /**
   * Return a list of actions matching a pattern.
   *
   * @param elementPath the path to match for
   * @return list of matching actions
   */
  List<Action> matchActions(ElementPath elementPath);
}
