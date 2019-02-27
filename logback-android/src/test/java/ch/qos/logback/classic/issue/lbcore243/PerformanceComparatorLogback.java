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
package ch.qos.logback.classic.issue.lbcore243;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Results AMD Phenom II X6 1110T processor and SSD disk
//Logback  with    immediate flush: 8356 nanos per call
//Logback  without immediate flush: 1758 nanos per call

public class PerformanceComparatorLogback {
  static Logger logbacklogger = LoggerFactory.getLogger(PerformanceComparatorLogback.class);

  public static void main(String[] args) throws JoranException, InterruptedException {
    initLogbackWithoutImmediateFlush();
    logbackParametrizedDebugCall();

    initLogbackWithImmediateFlush();
    logbackParametrizedDebugCall();
    System.out.println("###############################################");
    System.out.println("Logback  with    immediate flush: " + logbackParametrizedDebugCall() + " nanos per call");

    initLogbackWithoutImmediateFlush();
    System.out.println("Logback  without immediate flush: " + logbackParametrizedDebugCall() + " nanos per call");

    System.out.println("###############################################");
  }

  private static long logbackParametrizedDebugCall() {

    Integer j = 2;
    long start = System.nanoTime();
    for (int i = 0; i < Common.loop; i++) {
      logbacklogger.debug("SEE IF THIS IS LOGGED {}.", j);
    }
    return (System.nanoTime() - start) / Common.loop;
  }

  static String DIR_PREFIX = "src/test/java/ch/qos/logback/classic/issue/lbcore243/";


  static void configure(String file)  throws JoranException {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      JoranConfigurator jc = new JoranConfigurator();
      jc.setContext(loggerContext);
      loggerContext.reset();
      jc.doConfigure(file);
  }


  private static void initLogbackWithoutImmediateFlush() throws JoranException {
    configure(DIR_PREFIX + "logback_without_immediateFlush.xml");
  }

  private static void initLogbackWithImmediateFlush() throws JoranException {
    configure(DIR_PREFIX + "logback_with_immediateFlush.xml");
  }
}