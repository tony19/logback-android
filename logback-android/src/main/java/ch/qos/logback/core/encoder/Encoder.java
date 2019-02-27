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
package ch.qos.logback.core.encoder;

import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Encoders are responsible for transform an incoming event into a byte array
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Joen Huxhorn
 * @author Maarten Bosteels
 *
 * @param <E>
 *          event type
 * @since 0.9.19
 */
public interface Encoder<E> extends ContextAware, LifeCycle {

  /**
   * Get header bytes. This method is typically called upon opening of
   * an output stream.
   *
   * @return header bytes. Null values are allowed.
   */
  byte[] headerBytes();

  /**
   * Encode an event as bytes.
   *
   * @param event the log event
   */
  byte[] encode(E event);

  /**
   * Get footer bytes. This method is typically called prior to the closing
   * of the stream where events are written.
   *
   * @return footer bytes. Null values are allowed.
   */
  byte[] footerBytes();
}
