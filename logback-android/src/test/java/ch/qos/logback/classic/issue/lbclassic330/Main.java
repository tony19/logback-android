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
package ch.qos.logback.classic.issue.lbclassic330;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

  static Logger logger = LoggerFactory.getLogger(Main.class);
   static String DIR_PREFIX = "src/test/java/ch/qos/logback/classic/issue/lbclassic330/";

   public static void main(String[] args) throws JoranException, InterruptedException {
     init(DIR_PREFIX + "logback.xml");
     logger.debug("hello");
   }


   static void init(String file) throws JoranException {
     LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
     JoranConfigurator jc = new JoranConfigurator();
     jc.setContext(loggerContext);
     loggerContext.reset();
     jc.doConfigure(file);
   }
}
