/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.net;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.net.SocketAppenderTest.SocketAppenderFriend;
import ch.qos.logback.classic.net.mock.MockSyslogServer;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.net.SyslogConstants;
import ch.qos.logback.core.recovery.RecoveryCoordinator;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.StatusPrinter;

public class SyslogAppenderTest {

  LoggerContext lc = new LoggerContext();
  SyslogAppenderFriend sa;// = new SyslogAppenderFriend();
  MockSyslogServer mockServer;
  String loggerName = this.getClass().getName();
  Logger logger = lc.getLogger(loggerName);

  @Before
  public void setUp() throws Exception {
    lc.setName("test");
    sa = new SyslogAppenderFriend();
    sa.setContext(lc);
  }

  @After
  public void tearDown() throws Exception {
  }

  public void setMockServerAndConfigure(int expectedCount) throws InterruptedException {
    setMockServerAndConfigure(expectedCount, true);
  }

  public void setMockServerAndConfigure(int expectedCount, boolean start)
      throws InterruptedException {
    int port = RandomUtil.getRandomServerPort();

    mockServer = new MockSyslogServer(expectedCount, port);
    mockServer.start();
    // give MockSyslogServer head start

    Thread.sleep(100);

    sa.setSyslogHost("localhost");
    sa.setFacility("MAIL");
    sa.setPort(port);
    sa.setSuffixPattern("[%thread] %logger %msg");
    sa.setStackTracePattern("[%thread] foo "+CoreConstants.TAB);
    if (start) {
      sa.start();
      assertTrue(sa.isStarted());
    }
    String loggerName = this.getClass().getName();
    Logger logger = lc.getLogger(loggerName);
    logger.addAppender(sa);

  }

  @Test
  public void basic() throws InterruptedException {

    setMockServerAndConfigure(1);
    String logMsg = "hello";
    logger.debug(logMsg);

    // wait max 8 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);

    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);

    String threadName = Thread.currentThread().getName();

    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String first = "<\\d{2}>\\w{3} \\d{2} \\d{2}(:\\d{2}){2} [\\w.-]* ";
    checkRegexMatch(msg, first + "\\[" + threadName + "\\] " + loggerName + " "
        + logMsg);

  }

  @Test
  public void tException() throws InterruptedException {
    setMockServerAndConfigure(21);

    String logMsg = "hello";
    String exMsg = "just testing";
    Exception ex = new Exception(exMsg);
    logger.debug(logMsg, ex);
    StatusPrinter.print(lc);

    // wait max 2 seconds for mock server to finish. However, it should
    // much sooner than that.
    mockServer.join(8000);
    assertTrue(mockServer.isFinished());

    // message + 20 lines of stacktrace
    assertEquals(21, mockServer.getMessageList().size());
    // int i = 0;
    // for (String line: mockServer.msgList) {
    // System.out.println(i++ + ": " + line);
    // }

    String msg = mockServer.getMessageList().get(0);
    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));

    String expectedPrefix = "<\\d{2}>\\w{3} \\d{2} \\d{2}(:\\d{2}){2} [\\w.-]* ";
    String threadName = Thread.currentThread().getName();
    String regex = expectedPrefix + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg;
    checkRegexMatch(msg, regex);

    msg = mockServer.getMessageList().get(1);
    assertTrue(msg.contains(ex.getClass().getName()));
    assertTrue(msg.contains(ex.getMessage()));

    msg = mockServer.getMessageList().get(2);
    assertTrue(msg.startsWith(expected));
    regex = expectedPrefix + "\\[" + threadName + "\\] " +  "foo "+CoreConstants.TAB + "at ch\\.qos.*";
    checkRegexMatch(msg, regex);
  }

  private void checkRegexMatch(String s, String regex) {
    assertTrue("The string [" + s + "] did not match regex [" + regex + "]", s
        .matches(regex));
  }

  @Test
  public void large() throws InterruptedException {
    setMockServerAndConfigure(1);
    StringBuilder largeBuf = new StringBuilder();
    for (int i = 0; i < 2 * 1024 * 1024; i++) {
      largeBuf.append('a');
    }
    logger.debug(largeBuf.toString());

    String logMsg = "hello";
    logger.debug(logMsg);
    Thread.sleep(RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN+10);
    logger.debug(logMsg);
    
    mockServer.join(8000);
    assertTrue(mockServer.isFinished());
    
    // the first message is wasted
    assertEquals(1, mockServer.getMessageList().size());
    String msg = mockServer.getMessageList().get(0);
    String expected = "<"
        + (SyslogConstants.LOG_MAIL + SyslogConstants.DEBUG_SEVERITY) + ">";
    assertTrue(msg.startsWith(expected));
    String expectedPrefix = "<\\d{2}>\\w{3} \\d{2} \\d{2}(:\\d{2}){2} [\\w.-]* ";
    String threadName = Thread.currentThread().getName();
    String regex = expectedPrefix + "\\[" + threadName + "\\] " + loggerName
        + " " + logMsg;
    checkRegexMatch(msg, regex);
  }

  @Test
  public void LBCLASSIC_50() throws JoranException {

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    lc.reset();
    configurator.doConfigure(ClassicTestConstants.JORAN_INPUT_PREFIX
        + "syslog_LBCLASSIC_50.xml");

    org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("hello");
  }

  @Test
  public void unlazyAppenderIsConnectedAtStart() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(false);
    assertFalse(sa.isInitialized());
    sa.start();
    assertTrue(sa.isInitialized());
  }

  @Test
  public void lazyAppenderIsNotConnectedAtStart() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    assertFalse(sa.isInitialized());
    sa.start();
    assertFalse(sa.isInitialized());
  }

  @Test
  public void lazyAppenderIsConnectedOnAppend() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    assertFalse(sa.isInitialized());
    sa.start();
    assertFalse(sa.isInitialized());

    logger.debug("hello world");
    mockServer.join(8000);

    assertTrue(sa.isInitialized());
  }

  @Test
  public void lazyAppenderMessageIsDelivered() throws InterruptedException {
    setMockServerAndConfigure(1, false);
    sa.setLazy(true);
    sa.start();

    Marker marker = MarkerFactory.getMarker("testMarker");
    assertFalse(sa.isInitialized());
    assertTrue(mockServer.getMessageList().isEmpty());
    logger.debug(marker, "test msg");
    assertTrue(sa.isInitialized());

    mockServer.join(8000);
    assertTrue(mockServer.isFinished());
    assertEquals(1, mockServer.getMessageList().size());
  }

  class SyslogAppenderFriend extends SyslogAppender {
    public boolean isInitialized() {
      return this.sos != null;
    }
  }
}
