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
 * Useful scaffolding to measure the throughput of certain operations when
 * invoked by multiple threads.
 * 
 * @author Joern Huxhorn
 * @author Ralph Goers
 * @author Ceki Gulcu
 */
public class ThreadedThroughputCalculator extends MultiThreadedHarness {


  public ThreadedThroughputCalculator(long overallDurationInMillis) {
    super(overallDurationInMillis);
  }

  public void printThroughput(String msg) throws InterruptedException {
    printThroughput(msg, false);
  }
  
  public void printThroughput(String msg, boolean detailed) throws InterruptedException {
    long sum = 0;
    for (RunnableWithCounterAndDone r : runnableArray) {
      if(detailed) {
        System.out.println(r +" count="+r.getCounter());
      }
      sum += r.getCounter();
    }
    
    System.out.println(msg + "total of " + sum + " operations, or "
        + ((sum) / overallDurationInMillis) + " operations per millisecond");
  }
}
