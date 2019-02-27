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
package ch.qos.logback.classic.issue.lbcore_155;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class Main {

  public static void main(String[] args) throws InterruptedException {

    Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
    StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());
    OThread ot = new OThread();
    ot.start();
    Thread.sleep(OThread.WAIT_MILLIS-500);
    logger.info("About to interrupt");
    ot.interrupt();
    logger.info("After interrupt");
    logger.info("Leaving main");

  }
}
