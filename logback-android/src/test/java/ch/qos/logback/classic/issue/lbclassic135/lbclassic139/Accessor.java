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
package ch.qos.logback.classic.issue.lbclassic135.lbclassic139;

import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.contention.RunnableWithCounterAndDone;

/**
 * 
 * @author Olivier Cailloux
 * 
 */
public class Accessor extends RunnableWithCounterAndDone {
  private Logger logger;
  final Worker worker;
  final LoggerContext loggerContext;

  
  Accessor(Worker worker, LoggerContext lc) {
    this.worker = worker;
    this.loggerContext = lc;
    logger = lc.getLogger(this.getClass());
  }

  public void run() {
    print("entered run()");
    //Thread.yield();
    while (!isDone()) {
      logger.info("Current worker status is: {}.", worker);
    }
    print("leaving run()");
  }
  
  void print(String msg) {
    String thread = Thread.currentThread().getName();
    System.out.println("["+thread+"] "+msg);
  }
}
