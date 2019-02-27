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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;


public class LoggerContextAwareBase extends ContextAwareBase implements LoggerContextAware {
  
  /**
   * Set the owning context. The owning context cannot be set more than
   * once.
   */
  public void setLoggerContext(LoggerContext context) {
    super.setContext(context);
  }

  public void setContext(Context context) {
    // check that the context is of type LoggerContext. Otherwise, throw an exception
    // Context == null is a degenerate case but nonetheless permitted.
    if(context instanceof LoggerContext || context == null) {
      super.setContext(context);
    } else {
      throw new IllegalArgumentException("LoggerContextAwareBase only accepts contexts of type c.l.classic.LoggerContext");
    }
  }

  /**
   * Return the {@link LoggerContext} this component is attached to.
   * 
   * @return The owning LoggerContext
   */
  public LoggerContext getLoggerContext() {
    return (LoggerContext) context;
  }
  
}
