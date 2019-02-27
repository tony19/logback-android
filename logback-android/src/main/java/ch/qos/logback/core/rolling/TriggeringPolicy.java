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

import java.io.File;

import ch.qos.logback.core.spi.LifeCycle;


/**
 * A <code>TriggeringPolicy</code> controls the conditions under which roll-over
 * occurs. Such conditions include time of day, file size, an 
 * external event, the log request or a combination thereof.
 *
 * @author Ceki G&uuml;lc&uuml;
 * */

public interface TriggeringPolicy<E> extends LifeCycle {
  
  /**
   * Should roll-over be triggered at this time?
   * 
   * @param activeFile A reference to the currently active log file. 
   * @param event A reference to the currently event. 
   * @return true if a roll-over should occur.
   */
  boolean isTriggeringEvent(final File activeFile, final E event);
}
