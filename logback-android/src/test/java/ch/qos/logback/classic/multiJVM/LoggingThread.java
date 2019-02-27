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
package ch.qos.logback.classic.multiJVM;

import org.slf4j.Logger;

public class LoggingThread extends Thread {
  static String msgLong = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  final long len;
  final Logger logger;
  private double durationPerLog;

  public LoggingThread(Logger logger, long len) {
    this.logger = logger;
    this.len = len;
  }

  public void run() {
    long before = System.nanoTime();
    for (int i = 0; i < len; i++) {
      logger.debug(msgLong + " " + i);
//      try {
//        Thread.sleep(100);
//      } catch (InterruptedException e) {
//      }
    }
    // in microseconds
    durationPerLog = (System.nanoTime() - before) / (len * 1000.0);
  }

  public double getDurationPerLogInMicroseconds() {
    return durationPerLog;
  }
  
  
}
