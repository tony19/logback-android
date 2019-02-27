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
package ch.qos.logback.classic.issue.lbclassic36;

import java.text.SimpleDateFormat;
//import org.joda.time.format.DateTimeFormat;
//import org.joda.time.format.DateTimeFormatter;

import ch.qos.logback.core.contention.RunnableWithCounterAndDone;

/**
 * A runnable which behaves differently depending on the desired locking model.
 * 
 * @author Raplh Goers
 * @author Ceki Gulcu
 */
public class SelectiveDateFormattingRunnable extends
    RunnableWithCounterAndDone {

  public static final String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

  enum FormattingModel {
    SDF, JODA;
  }

  FormattingModel model;
  static long CACHE = 0;

  static SimpleDateFormat SDF = new SimpleDateFormat(ISO8601_PATTERN);
//  static final DateTimeFormatter JODA = DateTimeFormat
//      .forPattern(ISO8601_PATTERN);

  SelectiveDateFormattingRunnable(FormattingModel model) {
    this.model = model;
  }

  public void run() {
    switch (model) {
    case SDF:
      sdfRun();
      break;
    case JODA:
      jodaRun();
      break;
    }
  }

  void sdfRun() {

    for (;;) {
      synchronized (SDF) {
        long now = System.currentTimeMillis();
        if (CACHE != now) {
          CACHE = now;
          SDF.format(now);
        }
      }
      counter++;
      if (done) {
        return;
      }
    }
  }

  void jodaRun() {
    for (;;) {
      long now = System.currentTimeMillis();
      if (isCacheStale(now)) {
        //JODA.print(now);
      }
      counter++;
      if (done) {
        return;
      }
    }
  }
  
  private static boolean isCacheStale(long now) {
//    synchronized (JODA) {
//      if (CACHE != now) {
//        CACHE = now;
//        return true;
//      }
//    }
    return false;
  }

}
