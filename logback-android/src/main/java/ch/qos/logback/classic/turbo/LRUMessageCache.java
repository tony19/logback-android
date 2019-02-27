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
package ch.qos.logback.classic.turbo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clients of this class should only use the  {@link #getMessageCountAndThenIncrement} method. Other methods inherited
 * via LinkedHashMap are not thread safe.
 */
class LRUMessageCache extends LinkedHashMap<String, Integer> {

  private static final long serialVersionUID = 1L;
  final int cacheSize;

  LRUMessageCache(int cacheSize) {
    super((int) (cacheSize * (4.0f / 3)), 0.75f, true);
    if (cacheSize < 1) {
      throw new IllegalArgumentException("Cache size cannot be smaller than 1");
    }
    this.cacheSize = cacheSize;
  }

  int getMessageCountAndThenIncrement(String msg) {
    // don't insert null elements
    if (msg == null) {
      return 0;
    }

    Integer i;
    // LinkedHashMap is not LinkedHashMap. See also LBCLASSIC-255
    synchronized (this) {
      i = super.get(msg);
      if (i == null) {
        i = 0;
      } else {
        i = i + 1;
      }
      super.put(msg, i);
    }
    return i;
  }

  // called indirectly by get() or put() which are already supposed to be
  // called from within a synchronized block
  protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
    return (size() > cacheSize);
  }

  @Override
  synchronized public void clear() {
    super.clear();
  }
}
