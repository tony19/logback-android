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

}
