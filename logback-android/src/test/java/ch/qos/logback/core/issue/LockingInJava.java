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

/**
 * Short sample code illustrating locking policies in the JDK. See
 * http://jira.qos.ch/browse/LBCORE-97 for a discussion.
 * 
 * @author Joern Huxhorn
 * @author Ceki Gulcu
 */
public class LockingInJava implements Runnable {

  static int THREAD_COUNT = 5;
  static Object LOCK = new Object();
  static LockingInJava[] RUNNABLE_ARRAY = new LockingInJava[THREAD_COUNT];
  static Thread[] THREAD_ARRAY = new Thread[THREAD_COUNT];

  private int counter = 0;
  private boolean done = false;
  
  public static void main(String args[]) throws InterruptedException {
    printEnvironmentInfo();
    execute();
    printResults();
  }

  public static void printEnvironmentInfo() {
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

  public static void execute() throws InterruptedException {
    for (int i = 0; i < THREAD_COUNT; i++) {
      RUNNABLE_ARRAY[i] = new LockingInJava();
      THREAD_ARRAY[i] = new Thread(RUNNABLE_ARRAY[i]);
    }
    for (Thread t : THREAD_ARRAY) {
      t.start();
    }
    // let the threads run for a while
    Thread.sleep(10000);
    
    for (int i = THREAD_COUNT - 1; i <= 0; i--) {
      RUNNABLE_ARRAY[i].done = true;
    }
 
  }

  public static void printResults() {
    for (int i = 0; i < RUNNABLE_ARRAY.length; i++) {
      System.out.println("runnable[" + i + "]: " + RUNNABLE_ARRAY[i]);
    }
  }

  public void run() {
    for (;;) {
      synchronized (LOCK) {
        counter++;
        try {
          Thread.sleep(10);
        } catch (InterruptedException ex) {
        }
        if(done) {
          return;
        }
      }
    }
  }

  public String toString() {
    return "counter=" + counter;
  }

}
