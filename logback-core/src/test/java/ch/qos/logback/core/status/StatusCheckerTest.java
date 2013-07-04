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
package ch.qos.logback.core.status;

import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class StatusCheckerTest {

  private Context context = new ContextBase();
  private StatusChecker checker = new StatusChecker(context);

  @Test
  public void emptyStatusListShouldResultInNotFound() {
    assertEquals(-1, timeOfLastReset());
  }

  @Test
  public void withoutResetsCheckerShouldReturnNotFound() {
    context.getStatusManager().add(new InfoStatus("test", this));
    assertEquals(-1, timeOfLastReset());
  }

  @Test
  public void statusListShouldReturnLastResetTime() {
    context.getStatusManager().add(new InfoStatus("test", this));
    long resetTime = System.currentTimeMillis();
    context.getStatusManager().add(new InfoStatus(CoreConstants.RESET_MSG_PREFIX, this));
    context.getStatusManager().add(new InfoStatus("bla", this));
    assertTrue(resetTime <= timeOfLastReset());
  }

  /**
   * Return the time of last reset. -1 if last reset time could not be found
   * @return  time of last reset or -1
   */
  private long timeOfLastReset() {
    List<Status> statusList = checker.sm.getCopyOfStatusList();
    if (statusList == null)
      return -1;

    int len = statusList.size();
    for (int i = len-1; i >= 0; i--) {
      Status s = statusList.get(i);
      if (CoreConstants.RESET_MSG_PREFIX.equals(s.getMessage())) {
        return s.getDate();
      }
    }
    return -1;
  }
}
