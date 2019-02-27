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
package ch.qos.logback.classic.util;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.TrivialStatusListener;

@RunWith(RobolectricTestRunner.class)
public class ContextInitializerTest {

  LoggerContext loggerContext = new LoggerContext();
  Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    System.clearProperty(CoreConstants.STATUS_LISTENER_CLASS);
  }

  @SuppressWarnings("deprecation")
  @Test
  @Ignore
  // this test works only if logback-test.xml or logback.xml files are on the classpath.
  // However, this is something we try to avoid in order to simplify the life
  // of users trying to follow the manual and logback-examples from an IDE
  public void reset() throws JoranException {
    {
      new ContextInitializer(loggerContext).autoConfig();
      Appender<ILoggingEvent> appender = root.getAppender("STDOUT");
      assertNotNull(appender);
      assertTrue(appender instanceof ConsoleAppender);
    }
    {
      loggerContext.stop();
      Appender<ILoggingEvent> appender = root.getAppender("STDOUT");
      assertNull(appender);
    }
  }

  @Test
  public void autoConfigFromSystemProperties() throws JoranException  {
    doAutoConfigFromSystemProperties(ClassicTestConstants.INPUT_PREFIX + "autoConfig.xml");
    doAutoConfigFromSystemProperties("autoConfigAsResource.xml");
    // test passing a URL. note the relative path syntax with file:src/test/...
    doAutoConfigFromSystemProperties("file:"+ClassicTestConstants.INPUT_PREFIX + "autoConfig.xml");
  }

  public void doAutoConfigFromSystemProperties(String val) throws JoranException {
    //lc.reset();
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, val);
    new ContextInitializer(loggerContext).autoConfig();
    Appender<ILoggingEvent> appender = root.getAppender("AUTO_BY_SYSTEM_PROPERTY");
    assertNotNull(appender);
  }

  @Test
  public void autoStatusListener() throws JoranException {
    System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, TrivialStatusListener.class.getName());
    List<StatusListener> statusListenerList = loggerContext.getStatusManager().getCopyOfStatusListenerList();
    assertEquals(0, statusListenerList.size());
    doAutoConfigFromSystemProperties(ClassicTestConstants.INPUT_PREFIX + "autoConfig.xml");
    statusListenerList = loggerContext.getStatusManager().getCopyOfStatusListenerList();
    assertTrue(statusListenerList.size() +" should be 1", statusListenerList.size() == 1);
    // LOGBACK-767
    TrivialStatusListener tsl = (TrivialStatusListener) statusListenerList.get(0);
    assertTrue("expecting at least one event in list", tsl.list.size() > 0);
  }
}
