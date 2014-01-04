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
package ch.qos.logback.matchers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.hamcrest.Matcher;

import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * Matchers for working with {@link ILoggingEvent}s.
 *
 * @author Sebastian Gr&ouml;bler
 */
public final class LoggingEventMatchers {

  /**
   * Matches an {@link ILoggingEvent} which has a {@code message} which contains the given {@code message}.
   *
   * @param message the message which is expected to be contained in the event's message
   * @return a new {@link Matcher} instance
   */
  public static Matcher<ILoggingEvent> containsMessage(final String message) {
    return new LoggingEventMessageMatcher(containsString(message));
  }

  /**
   * Matches an {@link ILoggingEvent} which has a {@code message} which matches the given {@code matcher}.
   *
   * @param matcher the matcher for the {@code message}
   * @return a new {@link Matcher} instance
   */
  public static Matcher<ILoggingEvent> containsMessage(final Matcher<String> matcher) {
    return new LoggingEventMessageMatcher(matcher);
  }
}
