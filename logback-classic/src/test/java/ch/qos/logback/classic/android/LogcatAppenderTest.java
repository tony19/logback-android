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

import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

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
      encoder.start();

      logcatAppender.setEncoder(encoder);
    }
  }

  @BeforeClass
  static public void beforeClass() {
    params = new Params();
  }

  @Test
  public void testTagLengthLimit() {
    params.logcatAppender.addError("test error msg");
  }
}
