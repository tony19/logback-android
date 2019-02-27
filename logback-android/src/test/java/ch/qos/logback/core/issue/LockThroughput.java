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
package ch.qos.logback.core.issue;

import ch.qos.logback.core.contention.ThreadedThroughputCalculator;
import ch.qos.logback.core.issue.SelectiveLockRunnable.LockingModel;

/**
 * Short sample code testing the throughput of a fair lock.
 * 
 * @author Joern Huxhorn
 * @author Ceki Gulcu
 */
public class LockThroughput {

  static int THREAD_COUNT = 10;
  static long OVERALL_DURATION_IN_MILLIS = 5000;

  public static void main(String args[]) throws InterruptedException {

    ThreadedThroughputCalculator tp = new ThreadedThroughputCalculator(
        OVERALL_DURATION_IN_MILLIS);
    tp.printEnvironmentInfo("LockThroughput");

    for (int i = 0; i < 2; i++) {
      tp.execute(buildArray(LockingModel.SYNC));
      tp.execute(buildArray(LockingModel.UNFAIR));
      tp.execute(buildArray(LockingModel.FAIR));
    }
    
    tp.execute(buildArray(LockingModel.SYNC));
    tp.printThroughput("Sync:   ");

    tp.execute(buildArray(LockingModel.UNFAIR));
    tp.printThroughput("Unfair: ");

    tp.execute(buildArray(LockingModel.FAIR));
    tp.printThroughput("Fair:   ");
  }

  static SelectiveLockRunnable[] buildArray(LockingModel model) {
    SelectiveLockRunnable[] array = new SelectiveLockRunnable[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; i++) {
      array[i] = new SelectiveLockRunnable(model);
    }
    return array;
  }

}
