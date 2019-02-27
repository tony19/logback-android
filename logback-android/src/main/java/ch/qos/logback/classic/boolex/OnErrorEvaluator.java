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
package ch.qos.logback.classic.boolex;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;

/**
 * Evaluates to true when the logging event passed as parameter has level ERROR
 * or higher.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 */
public class OnErrorEvaluator extends EventEvaluatorBase<ILoggingEvent> {

  /**
   * Return true if event passed as parameter has level ERROR or higher, returns
   * false otherwise.
   */
  public boolean evaluate(ILoggingEvent event) throws NullPointerException,
      EvaluationException {
    return event.getLevel().levelInt >= Level.ERROR_INT;
  }
}
