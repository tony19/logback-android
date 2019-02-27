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
package ch.qos.logback.classic.turbo.lru;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An lru cache based on Java's LinkedHashMap.
 * 
 * @author Ceki Gulcu
 *
 * @param <K>
 * @param <V>
 */
public class X_LRUCache<K, V> extends LinkedHashMap<K, V> {
  private static final long serialVersionUID = -6592964689843698200L;

  final int cacheSize;

  public X_LRUCache(int cacheSize) {
    super((int) (cacheSize*(4.0f/3)), 0.75f, true);
    if(cacheSize < 1) {
      throw new IllegalArgumentException("Cache size cannnot be smaller than 1");
   } 
    this.cacheSize = cacheSize;
  }
  
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return (size() > cacheSize);
  }
  
  List<K> keyList() {
    ArrayList<K> al = new ArrayList<K>();
    al.addAll(keySet());
    return al;
  }
}
