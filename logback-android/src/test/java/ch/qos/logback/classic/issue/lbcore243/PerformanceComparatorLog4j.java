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

import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.log4j.xml.DOMConfigurator;

// WARNING This code compiles but does not measure anything useful because log4j-over-slf4j is a dependency. Log4j
// should be used instead

public class PerformanceComparatorLog4j {

   static org.apache.log4j.Logger log4jlogger = org.apache.log4j.Logger.getLogger(PerformanceComparatorLog4j.class);

   public static void main(String[] args) throws JoranException, InterruptedException {
     initLog4jWithoutImmediateFlush();

     // Let's run once for Just In Time compiler
     log4jDirectDebugCall();

     System.out.println("###############################################");
     System.out.println("Log4j    without immediate flush: " + log4jDirectDebugCall()+ " nanos per call");
     System.out.println("###############################################");
   }

   private static long log4jDirectDebugCall() {
     Integer j = 2;
     long start = System.nanoTime();
     for (int i = 0; i < Common.loop; i++) {
       log4jlogger.debug("SEE IF THIS IS LOGGED " + j + ".");
     }
     return (System.nanoTime() - start) / Common.loop;
   }

   static String DIR_PREFIX = "src/test/java/ch/qos/logback/classic/issue/lbcore243/";

   static void initLog4jWithoutImmediateFlush() {
     DOMConfigurator.configure(DIR_PREFIX + "log4j_without_immediateFlush.xml");
   }
   static void initLog4jWithImmediateFlush() {
     DOMConfigurator.configure(DIR_PREFIX + "log4j_with_immediateFlush.xml");
   }
}