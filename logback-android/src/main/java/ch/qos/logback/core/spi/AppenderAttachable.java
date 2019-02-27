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
