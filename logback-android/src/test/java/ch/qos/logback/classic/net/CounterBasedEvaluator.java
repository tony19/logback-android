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
package ch.qos.logback.classic.net;

import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * A simple EventEvaluator implementation that triggers email transmission after
 * a given number of events occur, regardless of event level.
 * 
 * <p>By default, the limit is 1024.
 */
public class CounterBasedEvaluator extends ContextAwareBase implements EventEvaluator<Object> {

  static int DEFAULT_LIMIT = 1024;
  int limit = DEFAULT_LIMIT;
  int counter = 0;
  String name;
  boolean started;

  public boolean evaluate(Object event) throws NullPointerException,
      EvaluationException {
    counter++;

    if (counter == limit) {
      counter = 0;
      return true;
    } else {
      return false;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isStarted() {
    return started;
  }

  public void start() {
    started = true;
  }

  public void stop() {
    started = false;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
  
  
}
