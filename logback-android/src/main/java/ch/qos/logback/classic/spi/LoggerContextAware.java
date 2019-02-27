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
import ch.qos.logback.core.spi.ContextAware;


public interface LoggerContextAware extends ContextAware {


  /** 
   * Set owning logger context for this component. This operation can
   * only be performed once. Once set, the owning context cannot be changed.
   *   
   * @param context The context where this component is attached.
   * @throws IllegalStateException If you try to change the context after it
   * has been set.
   **/
  void setLoggerContext(LoggerContext context);
 
}
