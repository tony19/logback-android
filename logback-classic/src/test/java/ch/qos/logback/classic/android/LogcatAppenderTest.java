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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

/**
 * Tests the {@link LogcatAppender} class
 *
 * @author Anthony Trinh
 */
public class LogcatAppenderTest {
  static private Params params;

  static class Params {
    LogcatAppender logcatAppender;
    LoggerContext context;

    Params() {
      logcatAppender = new LogcatAppender();
      context = new LoggerContext();
      logcatAppender.setContext(context);
      logcatAppender.setName("LOGCAT");

      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(context);
      encoder.setPattern("%msg");
      encoder.start();

      PatternLayoutEncoder tagEncoder = new PatternLayoutEncoder();
      tagEncoder.setContext(context);
      tagEncoder.setPattern("%msg");
      tagEncoder.start();

      logcatAppender.setTagEncoder(tagEncoder);
      logcatAppender.setEncoder(encoder);
      logcatAppender.start();
    }
  }

  @Before
  public void before() {
    params = new Params();
  }

  @Test
  public void tagTruncatedWhenTagLimitExceeded() {
    LoggingEvent event = new LoggingEvent();
    final String TAG = "123456789012345678901234567890";
    final String EXPECTED_TAG = "1234567890123456789012*";
    event.setMessage(TAG);

    params.logcatAppender.setCheckLoggable(true);
    String actualTag = params.logcatAppender.getTag(event);
    assertEquals("tag was not truncated", EXPECTED_TAG, actualTag);
  }

  // Issue #34
  @Test
  public void tagExcludesStackTraces () {
    LoggingEvent event = new LoggingEvent();
    Throwable throwable = new Throwable("throwable message");
    ThrowableProxy tp = new ThrowableProxy(throwable);
    event.setThrowableProxy(tp);

    final String TAG = "test message";
    event.setMessage(TAG);

    String actualTag = params.logcatAppender.getTagEncoder().getLayout().doLayout(event);
    assertEquals(TAG, actualTag);
  }
}
