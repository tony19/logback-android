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
package ch.qos.logback.classic.rolling;

import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CachingDateFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.CoreTestConstants;

import static org.junit.Assert.assertTrue;

/**
 * Test that we can create time-stamped log files with the help of
 * the &lt;timestamp> element in configuration files.
 * 
 * @author Ceki G&uuml;lc&uuml;
 *
 */
@RunWith(RobolectricTestRunner.class)
public class UniqueFileTest {

  static String UNIK_DIFF = "UNIK_DIFF";

  LoggerContext lc = new LoggerContext();
  StatusChecker sc = new StatusChecker(lc);
  int diff = RandomUtil.getPositiveInt()%1000;
  String diffAsStr = Integer.toString(diff);

  @Before
  public void setUp() {
    System.setProperty(UNIK_DIFF, diffAsStr);
  }

  @After
  public void tearDown() {
    System.clearProperty(UNIK_DIFF);
  }

  void loadConfig(String confifFile) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(lc);
    jc.doConfigure(confifFile);
  }

  @Test
  public void basic() throws Exception {
    loadConfig(ClassicTestConstants.JORAN_INPUT_PREFIX + "unique.xml");
    CachingDateFormatter sdf = new CachingDateFormatter("yyyyMMdd'T'HHmm");
    String timestamp = sdf.format(System.currentTimeMillis());

    sc.assertIsErrorFree();

    Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    root.info("hello");

    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX + "UNIK_" + timestamp + diffAsStr + "log.txt");
    assertTrue(file.exists());
  }
}
