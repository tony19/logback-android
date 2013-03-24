/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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

import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.Context;

import java.io.PrintStream;

/**
 * Print all new incoming status messages on the console (System.out).
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class OnConsoleStatusListener extends OnPrintStreamStatusListenerBase {

  @Override
  protected PrintStream getPrintStream() {
    return System.out;
  }

  /**
   * This utility method adds a new OnConsoleStatusListener to a context.
   * If the context's status manager is a {#link BasicStatusManager}, the
   * listener is added only if an OnConsoleStatusListener does not already
   * exist in the context.
   *
   * @param context
   * @since 1.0.1
   */
  static public void addNewInstanceToContext(Context context) {
    OnConsoleStatusListener onConsoleStatusListener = null;

    StatusManager sm = context.getStatusManager();
    if (sm instanceof BasicStatusManager) {
      onConsoleStatusListener = ((BasicStatusManager) sm).addConsoleStatusListenerIfAbsent(context);
    } else {
      onConsoleStatusListener = new OnConsoleStatusListener();
      onConsoleStatusListener.setContext(context);
      sm.add(onConsoleStatusListener);
    }

    if (onConsoleStatusListener != null) {
      onConsoleStatusListener.start();
    }
  }


}
