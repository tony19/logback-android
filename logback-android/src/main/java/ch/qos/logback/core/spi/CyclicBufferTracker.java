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

import ch.qos.logback.core.helpers.CyclicBuffer;

import java.util.*;

/**
 * CyclicBufferTracker tracks  {@link CyclicBuffer} instances.
 *
 * @author Ceki G&uuml;c&uuml;
 */
public class CyclicBufferTracker<E> extends AbstractComponentTracker<CyclicBuffer<E>> {

  static final int DEFAULT_NUMBER_OF_BUFFERS = 64;

  static final int DEFAULT_BUFFER_SIZE = 256;
  int bufferSize = DEFAULT_BUFFER_SIZE;


  public CyclicBufferTracker() {
    super();
    setMaxComponents(DEFAULT_NUMBER_OF_BUFFERS);
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  @Override
  protected void processPriorToRemoval(CyclicBuffer<E> component) {
    component.clear();
  }

  @Override
  protected CyclicBuffer<E> buildComponent(String key) {
    return  new CyclicBuffer<E>(bufferSize);
  }

  @Override
  protected boolean isComponentStale(CyclicBuffer<E> eCyclicBuffer) {
    return false;
  }

  // for testing purposes
  List<String> liveKeysAsOrderedList() {
    return new ArrayList<String>(liveMap.keySet());
  }

  List<String> lingererKeysAsOrderedList() {
    return new ArrayList<String>(lingerersMap.keySet());

  }

}
