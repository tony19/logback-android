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
package ch.qos.logback.core.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import ch.qos.logback.core.CoreConstants;

/**
 * Factory for {@link ch.qos.logback.core.net.ObjectWriter} instances.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class ObjectWriterFactory {

  /**
   * Creates a new {@link ch.qos.logback.core.net.AutoFlushingObjectWriter} instance.
   *
   * @param outputStream the underlying {@link java.io.OutputStream} to write to
   * @return a new {@link ch.qos.logback.core.net.AutoFlushingObjectWriter} instance
   * @throws IOException if an I/O error occurs while writing stream header
   */
  public AutoFlushingObjectWriter newAutoFlushingObjectWriter(OutputStream outputStream) throws IOException {
    return new AutoFlushingObjectWriter(new ObjectOutputStream(outputStream), CoreConstants.OOS_RESET_FREQUENCY);
  }
}
