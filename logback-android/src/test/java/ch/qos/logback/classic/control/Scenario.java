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
package ch.qos.logback.classic.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Scenario {

  private List<ScenarioAction> actionList = new Vector<ScenarioAction>();

  public void add(ScenarioAction action) {
    actionList.add(action);
  }

  public List<ScenarioAction> getActionList() {
    return new ArrayList<ScenarioAction>(actionList);
  }

  public int size() {
    return actionList.size();
  }

  public ScenarioAction get(int i) {
    return (ScenarioAction) actionList.get(i);
  }
}
