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
package ch.qos.logback.classic.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public class TurboFilterListTest {

  private final LoggerContext loggerContext = new LoggerContext();
  private final Logger logger = loggerContext.getLogger("a.b");
  private final List<Marker> markers = Collections.singletonList(MarkerFactory.getMarker("M"));
  private final Object[] params = new Object[] {1};
  private final Throwable throwable = new Exception("t");
  private final TurboFilterList list = new TurboFilterList();

  @Test
  public void emptyListIsNeutral() {
    assertEquals(FilterReply.NEUTRAL, decide(list));
  }

  @Test
  public void singleFilterDecidesAndReceivesTheCallArguments() {
    RecordingTurboFilter deny = new RecordingTurboFilter(FilterReply.DENY);
    list.add(deny);

    assertEquals(FilterReply.DENY, decide(list));
    assertEquals(1, deny.calls);
    assertSame(markers, deny.markers);
    assertSame(logger, deny.logger);
    assertSame(Level.INFO, deny.level);
    assertEquals("format", deny.format);
    assertSame(params, deny.params);
    assertSame(throwable, deny.t);
  }

  @Test
  public void singleFilterRemovedConcurrentlyIsNeutral() {
    // size() reports one filter, but it is removed before get(0)
    TurboFilterList racyList = spy(new TurboFilterList());
    doReturn(1).when(racyList).size();

    assertEquals(FilterReply.NEUTRAL, decide(racyList));
  }

  @Test
  public void firstDenyWinsAndStopsTheChain() {
    RecordingTurboFilter neutral = new RecordingTurboFilter(FilterReply.NEUTRAL);
    RecordingTurboFilter deny = new RecordingTurboFilter(FilterReply.DENY);
    RecordingTurboFilter accept = new RecordingTurboFilter(FilterReply.ACCEPT);
    list.add(neutral);
    list.add(deny);
    list.add(accept);

    assertEquals(FilterReply.DENY, decide(list));
    assertEquals(1, neutral.calls);
    assertEquals(1, deny.calls);
    assertEquals(0, accept.calls);
    assertSame(throwable, deny.t);
  }

  @Test
  public void firstAcceptWinsAndStopsTheChain() {
    RecordingTurboFilter neutral = new RecordingTurboFilter(FilterReply.NEUTRAL);
    RecordingTurboFilter accept = new RecordingTurboFilter(FilterReply.ACCEPT);
    RecordingTurboFilter deny = new RecordingTurboFilter(FilterReply.DENY);
    list.add(neutral);
    list.add(accept);
    list.add(deny);

    assertEquals(FilterReply.ACCEPT, decide(list));
    assertEquals(1, neutral.calls);
    assertEquals(1, accept.calls);
    assertEquals(0, deny.calls);
  }

  @Test
  public void allNeutralFiltersAreConsultedAndYieldNeutral() {
    RecordingTurboFilter first = new RecordingTurboFilter(FilterReply.NEUTRAL);
    RecordingTurboFilter second = new RecordingTurboFilter(FilterReply.NEUTRAL);
    list.add(first);
    list.add(second);

    assertEquals(FilterReply.NEUTRAL, decide(list));
    assertEquals(1, first.calls);
    assertEquals(1, second.calls);
  }

  private FilterReply decide(TurboFilterList tfl) {
    return tfl.getTurboFilterChainDecision(markers, logger, Level.INFO, "format", params, throwable);
  }

  static class RecordingTurboFilter extends TurboFilter {
    final FilterReply reply;
    int calls;
    List<Marker> markers;
    Logger logger;
    Level level;
    String format;
    Object[] params;
    Throwable t;

    RecordingTurboFilter(FilterReply reply) {
      this.reply = reply;
    }

    @Override
    public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format,
                              Object[] params, Throwable t) {
      calls++;
      this.markers = markers;
      this.logger = logger;
      this.level = level;
      this.format = format;
      this.params = params;
      this.t = t;
      return reply;
    }
  }
}
