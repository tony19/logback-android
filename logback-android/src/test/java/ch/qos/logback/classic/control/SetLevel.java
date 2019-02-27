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

import ch.qos.logback.classic.Level;

public class SetLevel extends ScenarioAction {
  final String loggerName;
  final Level level;

  public SetLevel(Level level, String loggerName) {
    this.level = level;
    this.loggerName = loggerName;
  }

  public Level getLevel() {
    return level;
  }

  public String getLoggerName() {
    return loggerName;
  }
  public String toString() {
    return "SetLevel("+level+", "+loggerName+")";
  }
}
