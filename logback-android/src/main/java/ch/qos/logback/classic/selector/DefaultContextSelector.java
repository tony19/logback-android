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
package ch.qos.logback.classic.selector;

import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;

public class DefaultContextSelector implements ContextSelector {

  private LoggerContext defaultLoggerContext;
  
  public DefaultContextSelector(LoggerContext context) {
    this.defaultLoggerContext = context;
  }
  
  public LoggerContext getLoggerContext() {
    return getDefaultLoggerContext();
  }

  public LoggerContext getDefaultLoggerContext() {
    return defaultLoggerContext;
  }

  public LoggerContext detachLoggerContext(String loggerContextName) {
    return defaultLoggerContext;
  }
  
  public List<String> getContextNames() {
    return Arrays.asList(defaultLoggerContext.getName());
  }
  
  public LoggerContext getLoggerContext(String name) {
    if (defaultLoggerContext.getName().equals(name)) {
      return defaultLoggerContext;
    } else {
      return null;
    }
  }
}
