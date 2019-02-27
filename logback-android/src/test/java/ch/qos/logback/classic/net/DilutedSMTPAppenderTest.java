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
package ch.qos.logback.classic.net;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import javax.mail.Address;
import javax.mail.MessagingException;

import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.spi.CyclicBufferTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Layout;

public class DilutedSMTPAppenderTest {

  SMTPAppender appender;
  CyclicBufferTracker<ILoggingEvent> cbTracker;
  CyclicBuffer<ILoggingEvent> cb;

  @Before
  public void setUp() throws Exception {
    LoggerContext lc = new LoggerContext();
    appender = new SMTPAppender();
    appender.setContext(lc);
    appender.setName("smtp");
    appender.setFrom("user@host.dom");
    appender.setLayout(buildLayout(lc));
    appender.setSMTPHost("mail2.qos.ch");
    appender.setSubject("logging report");
    appender.addTo("sebastien.nospam@qos.ch");
    appender.start();
    cbTracker = appender.getCyclicBufferTracker();
    cb = cbTracker.getOrCreate("", 0);

  }

  private static Layout<ILoggingEvent> buildLayout(LoggerContext lc) {
    PatternLayout layout = new PatternLayout();
    layout.setContext(lc);
    layout.setFileHeader("Some header\n");
    layout.setPattern("%-4relative [%thread] %-5level %class - %msg%n");
    layout.setFileFooter("Some footer");
    layout.start();
    return layout;
  }
  
  @After
  public void tearDown() throws Exception {
    appender = null;
  }

  @Test
  public void testStart() {
    assertEquals("sebastien.nospam@qos.ch%nopex", appender.getToAsListOfString().get(0));

    assertEquals("logging report", appender.getSubject());

    assertTrue(appender.isStarted());
  }

  @Test
  public void testAppendNonTriggeringEvent() {
    LoggingEvent event = new LoggingEvent();
    event.setThreadName("thead name");
    event.setLevel(Level.DEBUG);
    appender.subAppend(cb, event);
    assertEquals(1, cb.length());
  }

  @Test
  public void testEntryConditionsCheck() {
    appender.checkEntryConditions();
    assertEquals(0, appender.getContext().getStatusManager().getCount());
  }

  @Test
  public void testTriggeringPolicy() {
    appender.setEvaluator(null);
    appender.checkEntryConditions();
    assertEquals(1, appender.getContext().getStatusManager().getCount());
  }
  
  @Test
  public void testEntryConditionsCheckNoLayout() {
    appender.setLayout(null);
    appender.checkEntryConditions();
    assertEquals(1, appender.getContext().getStatusManager().getCount());
  }
  
  

  
}
