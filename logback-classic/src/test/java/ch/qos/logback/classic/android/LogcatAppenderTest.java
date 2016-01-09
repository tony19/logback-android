/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.android;

import android.util.Log;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

/**
 * Tests the {@link LogcatAppender} class
 *
 * @author Anthony Trinh
 */
@RunWith(RobolectricTestRunner.class)
public class LogcatAppenderTest {
  static private final String LOGGER_NAME = "LOGCAT";
  static private final int MAX_TAG_LENGTH = 23; // for android.util.Log.isLoggable()
  static private final String TAG = "123456789012345678901234567890";
  static private final String TRUNCATED_TAG = TAG.substring(0, MAX_TAG_LENGTH - 1) + "*";

  private LogcatAppender logcatAppender;
  private LoggerContext context = new LoggerContext();
  private Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

  @Before
  public void before() {
    context.reset();
    root.detachAndStopAllAppenders();
    configureLogcatAppender();
  }

  @Test
  public void longTagAllowedIfNotCheckLoggable() {
    LoggingEvent event = new LoggingEvent();
    event.setMessage(TAG);

    boolean checkLoggable = false;
    setTagPattern(TAG, checkLoggable);
    String actualTag = logcatAppender.getTag(event);

    assertThat(TRUNCATED_TAG, is(not(actualTag)));
    assertThat(TAG, is(actualTag));
  }

  @Test
  public void longTagTruncatedIfCheckLoggable() {
    LoggingEvent event = new LoggingEvent();
    event.setMessage(TAG);

    boolean checkLoggable = true;
    setTagPattern(TAG, checkLoggable);
    String actualTag = logcatAppender.getTag(event);

    assertThat(TRUNCATED_TAG, is(actualTag));
    assertThat(TAG, is(not(actualTag)));
  }

  // Issue #34
  @Test
  public void tagExcludesStackTraces() {
    // create logging event with throwable
    LoggingEvent event = new LoggingEvent();
    Throwable throwable = new Throwable("throwable");
    ThrowableProxy tp = new ThrowableProxy(throwable);
    event.setThrowableProxy(tp);
    event.setMessage(TAG);

    setTagPattern(TAG, true);

    // if the tags match, it does not include the stack trace
    String actualTag = logcatAppender.getTagEncoder().getLayout().doLayout(event);
    assertThat(TAG, is(actualTag));
  }

  private void setTagPattern(String tag, boolean checkLoggable) {
    logcatAppender.stop();
    logcatAppender.setCheckLoggable(checkLoggable);
    logcatAppender.getTagEncoder().setPattern(tag);
    logcatAppender.start();
  }

  private void configureLogcatAppender() {
    logcatAppender = new LogcatAppender();
    logcatAppender.setContext(context);
    logcatAppender.setName(LOGGER_NAME);

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%msg");
    encoder.start();

    PatternLayoutEncoder tagEncoder = new PatternLayoutEncoder();
    tagEncoder.setContext(context);
    tagEncoder.setPattern(TAG);
    tagEncoder.start();

    logcatAppender.setTagEncoder(tagEncoder);
    logcatAppender.setEncoder(encoder);
    logcatAppender.start();
  }


  private boolean logcatContains(List<ShadowLog.LogItem> logs, int level, String errorMessage) {
    boolean found = false;
    for (ShadowLog.LogItem s : logs) {
      if (level == s.type) {
        if (s.msg.contains(errorMessage)) {
          found = true;
          break;
        }
      }
    }
    return found;
  }

  private void assertLogcatContains(int level, String errorMessage) {
    List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag(LOGGER_NAME);
    assertThat(logs, is(notNullValue()));
    assertThat(logcatContains(logs, level, errorMessage), is(true));
  }

  private void addLogcatAppenderToRoot() {
    PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
    encoder2.setContext(context);
    encoder2.setPattern("[%thread] %method\\(\\): %msg%n");
    encoder2.start();

    LogcatAppender logcatAppender = new LogcatAppender();
    logcatAppender.setContext(context);
    logcatAppender.setName(LOGGER_NAME);
    logcatAppender.setEncoder(encoder2);
    logcatAppender.start();

    root.addAppender(logcatAppender);
  }

  /**
   * Issue #102
   */
  @Test
  public void logsExceptionWhenMessageTrailsWithNewline() {
    addLogcatAppenderToRoot();
    ShadowLog.reset();
    context.getLogger(LOGGER_NAME).debug("msg\n", new NullPointerException());
    assertLogcatContains(Log.DEBUG, NullPointerException.class.getName());
  }

  /**
   * Issue #102
   */
  @Test
  public void logsExceptionWhenMessageHasNoTrailingNewline() {
    addLogcatAppenderToRoot();
    ShadowLog.reset();
    context.getLogger(LOGGER_NAME).debug("msg", new NullPointerException());
    assertLogcatContains(Log.DEBUG, NullPointerException.class.getName());
  }
}
