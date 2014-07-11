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
package ch.qos.logback.core.spi;

import java.util.Iterator;

import ch.qos.logback.core.Appender;

/**
 * Interface for attaching appenders to objects.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public interface AppenderAttachable<E> {
  /**
   * Add an appender.
   * @param newAppender the appender to add
   */
  void addAppender(Appender<E> newAppender);

  /**
   * Get an iterator for appenders contained in the parent object.
   * @return the iterator
   */
  Iterator<Appender<E>> iteratorForAppenders();

  /**
   * Get an appender by name.
   * @param name name of appender to fetch
   * @return the appender, or null if not found
   */
  Appender<E> getAppender(String name);

  /**
   * Returns <code>true</code> if the specified appender is in list of
   * attached appenders, <code>false</code> otherwise.
   * @param appender the appender to check
   * @return true if appender is attached
   */
  boolean isAttached(Appender<E> appender);

  /**
   * Detach and processPriorToRemoval all previously added appenders.
   */
  void detachAndStopAllAppenders();

  /**
   * Detach the appender passed as parameter from the list of appenders.
   * @param appender the appender to detach
   * @return true if successful
   */
  boolean detachAppender(Appender<E> appender);

  /**
   * Detach the appender with the name passed as parameter from the list of
   * appenders.
   * @param name the name of the appender to detach
   * @return true if successful
   */
  boolean detachAppender(String name);
}
