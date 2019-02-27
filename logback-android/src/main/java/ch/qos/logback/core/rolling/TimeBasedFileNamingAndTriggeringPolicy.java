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
package ch.qos.logback.core.rolling;

import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.spi.ContextAware;

/**
 * This interface lists the set of methods that need to be implemented by
 * triggering policies which are nested within a {@link TimeBasedRollingPolicy}.
 *
 * @author Ceki G&uuml;lc&uuml;
 *
 * @param <E> type of log event object
 */
public interface TimeBasedFileNamingAndTriggeringPolicy<E> extends
    TriggeringPolicy<E>, ContextAware {

  /**
   * Set the host/parent {@link TimeBasedRollingPolicy}.
   *
   * @param tbrp
   *                parent TimeBasedRollingPolicy
   */
  void setTimeBasedRollingPolicy(TimeBasedRollingPolicy<E> tbrp);

  /**
   * Return the file name for the elapsed periods file name.
   *
   * @return the file name
   */
  String getElapsedPeriodsFileName();

  /**
   * Return the current periods file name without the compression suffix. This
   * value is equivalent to the active file name.
   *
   * @return current period's file name (without compression suffix)
   */
  String getCurrentPeriodsFileNameWithoutCompressionSuffix();

  /**
   * Return the archive remover appropriate for this instance.
   * @return the archive remover
   */
  ArchiveRemover getArchiveRemover();

  /**
   * Return the current time which is usually the value returned by
   * System.currentMillis(). However, for <b>testing</b> purposed this value
   * may be different than the real time.
   *
   * @return current time value
   */
  long getCurrentTime();

  /**
   * Set the current time. Only unit tests should invoke this method.
   *
   * @param now the current time in ms
   */
  void setCurrentTime(long now);
}
