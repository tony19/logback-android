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
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher which expects the {@link Status}' {@code level} to match a given {@code level}.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class StatusLevelMatcher extends TypeSafeMatcher<Status> {

  private final int level;

  public StatusLevelMatcher(int level) {
    this.level = level;
  }

  @Override
  public boolean matchesSafely(final Status status) {
    return level == status.getLevel();
  }

  @Override
  public void describeTo(final Description description) {
    final String levelString;
    switch(level) {
      case Status.INFO: levelString = "INFO"; break;
      case Status.WARN: levelString = "WARN"; break;
      case Status.ERROR:  levelString = "ERROR"; break;
      default: throw new IllegalStateException("Unexpected level: " + level);
    }

    description.appendText("level to be " + levelString);
  }
}
