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
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Matcher which expects the {@link Status}' {@code message} to match a given {@link Matcher}.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class StatusMessageMatcher extends TypeSafeMatcher<Status> {

  private Matcher<String> matcher;

  public StatusMessageMatcher(final Matcher<String> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matchesSafely(final Status status) {
    return matcher.matches(status.getMessage());
  }

  @Override
  public void describeTo(final Description description) {
    matcher.describeTo(description);
  }
}
