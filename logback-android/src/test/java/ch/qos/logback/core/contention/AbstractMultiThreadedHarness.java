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
package ch.qos.logback.core.contention;

abstract public class AbstractMultiThreadedHarness {

  RunnableWithCounterAndDone[] runnableArray;

  abstract public void waitUntilEndCondition() throws InterruptedException;

  public void execute(RunnableWithCounterAndDone[] runnableArray)
      throws InterruptedException {
    this.runnableArray = runnableArray;
    Thread[] threadArray = new Thread[runnableArray.length];

    for (int i = 0; i < runnableArray.length; i++) {
      threadArray[i] = new Thread(runnableArray[i], "Harness["+i+"]");
    }
    for (Thread t : threadArray) {
      t.start();
    }

    waitUntilEndCondition();
    for (RunnableWithCounterAndDone r : runnableArray) {
      r.setDone(true);
    }
    for (Thread t : threadArray) {
      t.join();
    }
  }
}
