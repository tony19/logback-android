/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusListener;

public class StatusListenerConfigHelper {

  public static void installIfAsked(Context context) {
    String slClass = OptionHelper.getSystemProperty(CoreConstants.STATUS_LISTENER_CLASS);
    if (!OptionHelper.isEmpty(slClass)) {
      addStatusListener(context, slClass);
    }
  }

  private static void addStatusListener(Context context, String listenerClassName) {
    StatusListener listener = createListenerPerClassName(context, listenerClassName);
    initAndAddListener(context, listener);
  }

  private static void initAndAddListener(Context context, StatusListener listener) {
    if (listener != null) {
      if (listener instanceof ContextAware) // LOGBACK-767
        ((ContextAware) listener).setContext(context);

      boolean effectivelyAdded = context.getStatusManager().add(listener);
      if (effectivelyAdded && (listener instanceof LifeCycle)) {
        ((LifeCycle) listener).start(); // LOGBACK-767
      }
    }
  }

  private static StatusListener createListenerPerClassName(Context context, String listenerClass) {
    try {
      return (StatusListener) OptionHelper.instantiateByClassName(listenerClass, StatusListener.class, context);
    } catch (Exception e) {
      // printing on the console is the best we can do
      e.printStackTrace();
      return null;
    }
  }

  /**
   * This utility method adds a new OnConsoleStatusListener to the context
   * passed as parameter.
   *
   * @param context
   * @since 1.0.1
   */
  static public void addOnConsoleListenerInstance(Context context, OnConsoleStatusListener onConsoleStatusListener) {
    onConsoleStatusListener.setContext(context);
    boolean effectivelyAdded = context.getStatusManager().add(onConsoleStatusListener);
    if (effectivelyAdded) {
      onConsoleStatusListener.start();
    }
  }

}
