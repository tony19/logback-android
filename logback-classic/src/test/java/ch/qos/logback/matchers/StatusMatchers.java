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

import ch.qos.logback.core.status.Status;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * Matchers for working with {@link Status}es.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class StatusMatchers {

  /**
   * Matches an iterable of {@link Status}es which has no item with a {@code message} containing the given {@code message}.
   *
   * @param message the message to not be contained in the items of the iterable
   * @return a new {@link Matcher} instance
   */
  public static Matcher<Iterable<Status>> hasNoItemWhichContainsMessage(final String message) {
    return not(hasItem(containsMessage(message)));
  }

  /**
   * Matches a {@link Status} which has a {@code message} containing the given {@code message}.
   *
   * @param message the expected message
   * @return a new {@link Matcher} instance
   */
  public static Matcher<Status> containsMessage(final String message) {
    return new StatusMessageMatcher(containsString(message));
  }

  /**
   * Matches a {@link Status} which has a {@code level} matching the given {@code level}.
   *
   * @param level the expected level
   * @return a new {@link Matcher} instance
   */
  public static Matcher<Status> hasLevel(final int level) {
    return new StatusLevelMatcher(level);
  }

  /**
   * Matches a {@link Status} which has a {@code throwable} which is an instance of the provided {@code throwableClass}.
   *
   * @param throwableClass the expected throwable class
   * @return a new {@link Matcher} instance
   */
  public static Matcher<Status> hasThrowable(final Class<? extends Throwable> throwableClass) {
    return new StatusThrowableMatcher(instanceOf(throwableClass));
  }
}
