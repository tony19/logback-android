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
package ch.qos.logback.core.read;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.helpers.CyclicBuffer;

/**
 * CyclicBufferAppender stores events in a cyclic buffer of user-specified size. As the
 * name suggests, if the size of the buffer is N, only the latest N events are available.
 *
 *
 * @author Ceki Gulcu
 */
public class CyclicBufferAppender<E> extends AppenderBase<E> {

  CyclicBuffer<E> cb;
  int maxSize = 512;

  public void start() {
    cb = new CyclicBuffer<E>(maxSize);
    super.start();
  }

  public void stop() {
    cb = null;
    super.stop();
  }

  @Override
  protected void append(E eventObject) {
    if (!isStarted()) {
      return;
    }
    cb.add(eventObject);
  }

  public int getLength() {
    if (isStarted()) {
      return cb.length();
    } else {
      return 0;
    }
  }

  public E get(int i) {
    if (isStarted()) {
      return cb.get(i);
    } else {
      return null;
    }
  }

  public void reset() {
    cb.clear();
  }

  /**
   * Gets the size of the cyclic buffer.
   * @return the size of the buffer
   */
  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

}
