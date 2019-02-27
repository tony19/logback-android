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

/**
 * Automatically flushes the underlying {@link java.io.ObjectOutputStream} immediately after calling
 * it's {@link java.io.ObjectOutputStream#writeObject(Object)} method.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class AutoFlushingObjectWriter implements ObjectWriter {

  private final ObjectOutputStream objectOutputStream;
  private final int resetFrequency;
  private int writeCounter = 0;

  /**
   * Creates a new instance for the given {@link java.io.ObjectOutputStream}.
   *
   * @param objectOutputStream the stream to write to
   * @param resetFrequency the frequency with which the given stream will be
   *                       automatically reset to prevent a memory leak
   */
  public AutoFlushingObjectWriter(ObjectOutputStream objectOutputStream, int resetFrequency) {
    this.objectOutputStream = objectOutputStream;
    this.resetFrequency = resetFrequency;
  }

  @Override
  public void write(Object object) throws IOException {
    objectOutputStream.writeObject(object);
    objectOutputStream.flush();
    preventMemoryLeak();
  }

  /**
   * Failing to reset the object output stream every now and then creates a serious memory leak which
   * is why the underlying stream will be reset according to the {@code resetFrequency}.
   */
  private void preventMemoryLeak() throws IOException {
    if (++writeCounter >= resetFrequency) {
      objectOutputStream.reset();
      writeCounter = 0;
    }
  }
}
