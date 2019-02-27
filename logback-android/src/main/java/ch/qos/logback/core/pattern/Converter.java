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
package ch.qos.logback.core.pattern;

/**
 * A minimal converter which sets up the general interface for derived classes.
 * It also implements the functionality to chain converters in a linked list.
 *
 * @author ceki
 */
abstract public class Converter<E> {

  Converter<E> next;

  /**
   * The convert method is responsible for extracting data from the event and
   * storing it for later use by the write method.
   *
   * @param event the log event
   * @return the string conversion
   */
  public abstract String convert(E event);

  /**
   * In its simplest incarnation, a convert simply appends the data extracted from
   * the event to the buffer passed as parameter.
   *
   * @param buf The input buffer where data is appended
   * @param event The event from where data is extracted
   */
  public void write(StringBuilder buf, E event) {
    buf.append(convert(event));
  }

  public final void setNext(Converter<E> next) {
    if (this.next != null) {
      throw  new IllegalStateException("Next converter has been already set");
    }
    this.next = next;
  }

  public final Converter<E> getNext() {
    return next;
  }
}
