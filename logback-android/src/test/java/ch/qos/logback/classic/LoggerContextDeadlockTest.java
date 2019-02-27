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
package ch.qos.logback.classic;

import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

@RunWith(RobolectricTestRunner.class)
public class LoggerContextDeadlockTest {

  LoggerContext loggerContext = new LoggerContext();
  JoranConfigurator jc = new JoranConfigurator();
  GetLoggerThread getLoggerThread = new GetLoggerThread(loggerContext);

  @Before
  public void setUp() throws Exception {
    jc.setContext(loggerContext);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test(timeout = 20000)
  public void testLBCLASSIC_81() throws JoranException {


    getLoggerThread.start();
    for (int i = 0; i < 500; i++) {
      ByteArrayInputStream baos = new ByteArrayInputStream(
              "<configuration><root level=\"DEBUG\"/></configuration>".getBytes());
      jc.doConfigure(baos);
    }
  }

  class GetLoggerThread extends Thread {

    final LoggerContext loggerContext;

    GetLoggerThread(LoggerContext loggerContext) {
      this.loggerContext = loggerContext;
    }

    @Override
    public void run() {
      for (int i = 0; i < 10000; i++) {
        if (i % 100 == 0) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
          }
        }
        loggerContext.getLogger("a" + i);
      }
    }
  }

}
