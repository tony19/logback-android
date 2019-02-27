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
package ch.qos.logback.classic.issue.logback416;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.issue.lbclassic135.LoggingRunnable;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.contention.MultiThreadedHarness;
import ch.qos.logback.core.contention.RunnableWithCounterAndDone;
import ch.qos.logback.core.joran.spi.JoranException;

@RunWith(RobolectricTestRunner.class)
public class ConcurrentSiftingTest {
  final static int THREAD_COUNT = 5;
  static String FOLDER_PREFIX = ClassicTestConstants.JORAN_INPUT_PREFIX
      + "sift/";

  LoggerContext loggerContext = new LoggerContext();
  protected Logger logger = loggerContext.getLogger(this.getClass().getName());
  protected Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

  int totalTestDuration = 50;
  MultiThreadedHarness harness = new MultiThreadedHarness(totalTestDuration);
  RunnableWithCounterAndDone[] runnableArray = buildRunnableArray();

  protected void configure(String file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(loggerContext);
    jc.doConfigure(file);
  }

  RunnableWithCounterAndDone[] buildRunnableArray() {
    RunnableWithCounterAndDone[] rArray = new RunnableWithCounterAndDone[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; i++) {
      rArray[i] = new LoggingRunnable(logger);
    }
    return rArray;
  }

  @Test
  public void concurrentAccess() throws JoranException, InterruptedException {
    configure(FOLDER_PREFIX + "logback_416.xml");
    harness.execute(runnableArray);
    assertEquals(1, InstanceCountingAppender.INSTANCE_COUNT.get());
  }
}
