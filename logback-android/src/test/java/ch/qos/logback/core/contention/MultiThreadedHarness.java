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


/**
 * Useful scaffolding/harness to start and processPriorToRemoval multiple threads.
 * 
 * @author Joern Huxhorn
 * @author Ralph Goers
 * @author Ceki Gulcu
 */
public class MultiThreadedHarness extends AbstractMultiThreadedHarness {

  final long overallDurationInMillis;

  public MultiThreadedHarness(long overallDurationInMillis) {
    this.overallDurationInMillis = overallDurationInMillis;
  }

  public void printEnvironmentInfo(String msg) {
    System.out.println("=== " + msg + " ===");
    System.out.println("java.runtime.version = "
        + System.getProperty("java.runtime.version"));
    System.out.println("java.vendor          = "
        + System.getProperty("java.vendor"));
    System.out.println("java.version         = "
        + System.getProperty("java.version"));
    System.out.println("os.name              = "
        + System.getProperty("os.name"));
    System.out.println("os.version           = "
        + System.getProperty("os.version"));
  }

  public void waitUntilEndCondition() throws InterruptedException {
    Thread.sleep(overallDurationInMillis);
  }
}
