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

import junit.framework.Assert;

import org.junit.Test;

public class LRUMessageCacheTest {

  @Test
  public void testEldestEntriesRemoval() {
    final LRUMessageCache cache = new LRUMessageCache(2);
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("0"));
    Assert.assertEquals(1, cache.getMessageCountAndThenIncrement("0"));
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("1"));
    Assert.assertEquals(1, cache.getMessageCountAndThenIncrement("1"));
    // 0 entry should have been removed.
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("2"));
    // So it is expected a returned value of 0 instead of 2.
    // 1 entry should have been removed.
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("0"));
    // So it is expected a returned value of 0 instead of 2.
    // 2 entry should have been removed.
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("1"));
    // So it is expected a returned value of 0 instead of 2.
    Assert.assertEquals(0, cache.getMessageCountAndThenIncrement("2"));
  }

}
