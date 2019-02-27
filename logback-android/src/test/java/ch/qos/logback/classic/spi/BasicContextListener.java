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
package ch.qos.logback.classic.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class BasicContextListener implements LoggerContextListener {

  enum UpdateType { NONE, START, RESET, STOP , LEVEL_CHANGE};
  
  UpdateType updateType = UpdateType.NONE;
  LoggerContext context;
  Logger logger;
  Level level;
  
  boolean resetResistant;
  
  public void setResetResistant(boolean resetResistant) {
    this.resetResistant = resetResistant;
  }
  
  public void onReset(LoggerContext context) {
    updateType =  UpdateType.RESET;
    this.context = context;
    
  }
  public void onStart(LoggerContext context) {
    updateType =  UpdateType.START;;
    this.context = context;
  }
  
  public void onStop(LoggerContext context) {
    updateType =  UpdateType.STOP;;
    this.context = context;
  }

  public boolean isResetResistant() {
    return resetResistant;
  }

  public void onLevelChange(Logger logger, Level level) {
    updateType = UpdateType.LEVEL_CHANGE;
    this.logger = logger;
    this.level = level;
  }
  
}
