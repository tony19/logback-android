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

import static junit.framework.Assert.*;

import java.lang.reflect.Field;

import org.junit.Test;

import ch.qos.logback.core.spi.FilterReply;

public class DuplicateMessageFilterTest {

  @Test
  public void smoke() {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    dmf.setAllowedRepetitions(0);
    dmf.start();
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "x", null,
        null));
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "y", null,
        null));
    assertEquals(FilterReply.DENY, dmf
        .decide(null, null, null, "x", null, null));
    assertEquals(FilterReply.DENY, dmf
        .decide(null, null, null, "y", null, null));
  }

  @Test
  public void memoryLoss() {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    dmf.setAllowedRepetitions(1);
    dmf.setCacheSize(1);
    dmf.start();
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "a", null,
        null));
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "b", null,
        null));
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "a", null,
        null));
  }

  @Test
  public void many() {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    dmf.setAllowedRepetitions(0);
    int cacheSize = 10;
    int margin = 2;
    dmf.setCacheSize(cacheSize);
    dmf.start();
    for (int i = 0; i < cacheSize + margin; i++) {
      assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "a" + i,
          null, null));
    }
    for (int i = cacheSize - 1; i >= margin; i--) {
      assertEquals(FilterReply.DENY, dmf.decide(null, null, null, "a" + i,
          null, null));
    }
    for (int i = margin - 1; i >= 0; i--) {
      assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "a" + i,
          null, null));
    }
  }

  @Test
  // isXXXEnabled invokes decide with a null format
  // http://jira.qos.ch/browse/LBCLASSIC-134
  public void nullFormat() {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    dmf.setAllowedRepetitions(0);
    dmf.setCacheSize(10);
    dmf.start();
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, null, null,
        null));
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, null, null,
        null));
  }

  @Test
  public void allowedRepetitionsAndCacheSizeHaveDefaultsAndCanBeSet() {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    assertEquals(5, dmf.getAllowedRepetitions());
    assertEquals(DuplicateMessageFilter.DEFAULT_ALLOWED_REPETITIONS, dmf.getAllowedRepetitions());
    assertEquals(100, dmf.getCacheSize());
    assertEquals(DuplicateMessageFilter.DEFAULT_CACHE_SIZE, dmf.getCacheSize());

    dmf.setAllowedRepetitions(3);
    dmf.setCacheSize(7);

    assertEquals(3, dmf.getAllowedRepetitions());
    assertEquals(7, dmf.getCacheSize());
  }

  @Test
  public void stopClearsAndDropsTheMessageCache() throws Exception {
    DuplicateMessageFilter dmf = new DuplicateMessageFilter();
    dmf.setAllowedRepetitions(0);
    dmf.start();
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "x", null, null));
    assertEquals(FilterReply.DENY, dmf.decide(null, null, null, "x", null, null));
    Field msgCacheField = DuplicateMessageFilter.class.getDeclaredField("msgCache");
    msgCacheField.setAccessible(true);
    LRUMessageCache msgCache = (LRUMessageCache) msgCacheField.get(dmf);
    assertFalse(msgCache.isEmpty());

    dmf.stop();

    assertFalse(dmf.isStarted());
    assertTrue(msgCache.isEmpty());
    assertNull(msgCacheField.get(dmf));

    // a restarted filter has forgotten the messages seen before
    dmf.start();
    assertEquals(FilterReply.NEUTRAL, dmf.decide(null, null, null, "x", null, null));
  }

}
