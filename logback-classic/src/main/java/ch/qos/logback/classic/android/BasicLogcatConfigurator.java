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

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

/**
 * BasicLogcatConfigurator configures logback-classic by attaching a
 * {@link LogcatAppender} to the root logger. The appender's layout is set to a
 * {@link ch.qos.logback.classic.PatternLayout} with the pattern "%msg".
 *
 * The equivalent default configuration in XML would be:
 * <pre>
 * &lt;configuration&gt;
 *  &lt;appender name="LOGCAT"
 *           class="ch.qos.logback.classic.android.LogcatAppender" &gt;
 *      &lt;checkLoggable&gt;false&lt;/checkLoggable&gt;
 *      &lt;encoder&gt;
 *          &lt;pattern&gt;%msg&lt;/pattern&gt;
 *      &lt;/encoder&gt;
 *  &lt;/appender&gt;
 *  &lt;root level="DEBUG" &gt;
 *     &lt;appender-ref ref="LOGCAT" /&gt;
 *  &lt;/root&gt;
 * &lt;/configuration&gt;
 * </pre>
 *
 * @author Anthony Trinh
 */
public class BasicLogcatConfigurator {

  private BasicLogcatConfigurator() {
  }

  public static void configure(LoggerContext lc) {
    StatusManager sm = lc.getStatusManager();
    if (sm != null) {
      sm.add(new InfoStatus("Setting up default configuration.", lc));
    }
    LogcatAppender appender = new LogcatAppender();
    appender.setContext(lc);
    appender.setName("logcat");

    // We don't need a trailing new-line character in the pattern
    // because logcat automatically appends one for us.
    PatternLayoutEncoder pl = new PatternLayoutEncoder();
    pl.setContext(lc);
    pl.setPattern("%msg");
    pl.start();

    appender.setEncoder(pl);
    appender.start();
    Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(appender);
  }

  public static void configureDefaultContext() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    configure(lc);
  }
}
