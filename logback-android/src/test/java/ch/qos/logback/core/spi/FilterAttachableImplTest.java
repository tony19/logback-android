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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.filter.Filter;

public class FilterAttachableImplTest {

  FilterAttachableImpl<Object> fai = new FilterAttachableImpl<Object>();
  Object event = new Object();

  @Test
  public void noFiltersMeansNeutral() {
    assertEquals(FilterReply.NEUTRAL, fai.getFilterChainDecision(event));
  }

  @Test
  public void neutralFiltersAreAllConsultedAndYieldNeutral() {
    RecordingFilter first = new RecordingFilter(FilterReply.NEUTRAL);
    RecordingFilter second = new RecordingFilter(FilterReply.NEUTRAL);
    fai.addFilter(first);
    fai.addFilter(second);

    assertEquals(FilterReply.NEUTRAL, fai.getFilterChainDecision(event));
    assertEquals(Arrays.asList(event), first.seen);
    assertEquals(Arrays.asList(event), second.seen);
  }

  @Test
  public void firstAcceptWinsAndStopsTheChain() {
    RecordingFilter neutral = new RecordingFilter(FilterReply.NEUTRAL);
    RecordingFilter accept = new RecordingFilter(FilterReply.ACCEPT);
    RecordingFilter deny = new RecordingFilter(FilterReply.DENY);
    fai.addFilter(neutral);
    fai.addFilter(accept);
    fai.addFilter(deny);

    assertEquals(FilterReply.ACCEPT, fai.getFilterChainDecision(event));
    assertEquals(1, neutral.seen.size());
    assertEquals(1, accept.seen.size());
    assertTrue(deny.seen.isEmpty());
  }

  @Test
  public void firstDenyWinsAndStopsTheChain() {
    RecordingFilter neutral = new RecordingFilter(FilterReply.NEUTRAL);
    RecordingFilter deny = new RecordingFilter(FilterReply.DENY);
    RecordingFilter accept = new RecordingFilter(FilterReply.ACCEPT);
    fai.addFilter(neutral);
    fai.addFilter(deny);
    fai.addFilter(accept);

    assertEquals(FilterReply.DENY, fai.getFilterChainDecision(event));
    assertEquals(1, neutral.seen.size());
    assertEquals(1, deny.seen.size());
    assertTrue(accept.seen.isEmpty());
  }

  @Test
  public void copyOfAttachedFiltersListHasTheFiltersInOrderAndIsDetached() {
    RecordingFilter first = new RecordingFilter(FilterReply.NEUTRAL);
    RecordingFilter second = new RecordingFilter(FilterReply.DENY);
    fai.addFilter(first);
    fai.addFilter(second);

    List<Filter<Object>> copy = fai.getCopyOfAttachedFiltersList();
    assertEquals(2, copy.size());
    assertSame(first, copy.get(0));
    assertSame(second, copy.get(1));

    copy.clear();
    assertEquals(2, fai.getCopyOfAttachedFiltersList().size());
  }

  @Test
  public void clearAllFiltersRemovesEveryFilter() {
    RecordingFilter deny = new RecordingFilter(FilterReply.DENY);
    fai.addFilter(deny);
    fai.addFilter(new RecordingFilter(FilterReply.ACCEPT));

    fai.clearAllFilters();

    assertTrue(fai.getCopyOfAttachedFiltersList().isEmpty());
    assertEquals(FilterReply.NEUTRAL, fai.getFilterChainDecision(event));
    assertTrue(deny.seen.isEmpty());
  }

  static class RecordingFilter extends Filter<Object> {
    final FilterReply reply;
    final List<Object> seen = new ArrayList<Object>();

    RecordingFilter(FilterReply reply) {
      this.reply = reply;
    }

    @Override
    public FilterReply decide(Object event) {
      seen.add(event);
      return reply;
    }
  }
}
