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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
  static private final int MAX_TAG_LENGTH = 23; // for android.util.Log.isLoggable()
  static private final String TAG = "123456789012345678901234567890";
  static private final String TRUNCATED_TAG = TAG.substring(0, MAX_TAG_LENGTH - 1) + "*";

  private LogcatAppender logcatAppender;
  private LoggerContext context;

  @Before
  public void before() {
    configureAppenderDirectly();
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

  private void configureAppenderDirectly() {
    context = new LoggerContext();
    logcatAppender = new LogcatAppender();
    logcatAppender.setContext(context);
    logcatAppender.setName("LOGCAT");

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
}
