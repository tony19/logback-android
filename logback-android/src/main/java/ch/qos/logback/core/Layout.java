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
package ch.qos.logback.core;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

public interface Layout<E> extends ContextAware, LifeCycle {

  /**
   * Transform an event (of type Object) and return it as a String after
   * appropriate formatting.
   *
   * <p>Taking in an object and returning a String is the least sophisticated
   * way of formatting events. However, it is remarkably CPU-effective.
   * </p>
   *
   * @param event The event to format
   * @return the event formatted as a String
   */
  String doLayout(E event);

  /**
   * Return the file header for this layout. The returned value may be null.
   * @return The header.
   */
  String getFileHeader();

  /**
   * Return the header of the logging event formatting. The returned value
   * may be null.
   *
   * @return The header.
   */
  String getPresentationHeader();

  /**
   * Return the footer of the logging event formatting. The returned value
   * may be null.
   *
   * @return The footer.
   */

  String getPresentationFooter();

  /**
   * Return the file footer for this layout. The returned value may be null.
   * @return The footer.
   */
  String getFileFooter();

  /**
   * Returns the content type as appropriate for the implementation.
   *
   * @return the content type
   */
  String getContentType();

}
