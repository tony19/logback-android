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

import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * A <code>RollingPolicy</code> is responsible for performing the rolling over
 * of the active log file. The <code>RollingPolicy</code> is also responsible
 * for providing the <em>active log file</em>, that is the live file where
 * logging output will be directed.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public interface RollingPolicy extends LifeCycle {

  /**
   * Rolls over log files according to implementation policy.
   *
   * <p>This method is invoked by {@link RollingFileAppender}, usually at the
   * behest of its {@link TriggeringPolicy}.
   *
   * @throws RolloverFailure
   *                 Thrown if the rollover operation fails for any reason.
   */
  void rollover() throws RolloverFailure;

  /**
   * Get the name of the active log file.
   *
   * <p>With implementations such as {@link TimeBasedRollingPolicy}, this
   * method returns a new file name, where the actual output will be sent.
   *
   * <p>On other implementations, this method might return the FileAppender's
   * file property.
   * @return the name of the active log file
   */
  String getActiveFileName();

  /**
   * The compression mode for this policy.
   *
   * @return the compression mode
   */
  CompressionMode getCompressionMode();

  /**
   * This method allows RollingPolicy implementations to be aware of their
   * containing appender.
   *
   * @param appender the associated file appender
   */

  void setParent(FileAppender<?> appender);
}
