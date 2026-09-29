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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

public class MatchingFilterTest {

  MatchingFilter filter = new MatchingFilter() {
    @Override
    public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params,
                              Throwable t) {
      return onMatch;
    }
  };

  @Test
  public void onMatchAndOnMismatchDefaultToNeutral() {
    assertEquals(FilterReply.NEUTRAL, filter.onMatch);
    assertEquals(FilterReply.NEUTRAL, filter.onMismatch);
  }

  @Test
  public void setOnMatchAcceptsNeutralAcceptAndDeny() {
    filter.setOnMatch("ACCEPT");
    assertEquals(FilterReply.ACCEPT, filter.onMatch);
    filter.setOnMatch("DENY");
    assertEquals(FilterReply.DENY, filter.onMatch);
    filter.setOnMatch("NEUTRAL");
    assertEquals(FilterReply.NEUTRAL, filter.onMatch);
    filter.setOnMatch("DENY");
    assertEquals(FilterReply.DENY, filter.onMatch);

    assertEquals(FilterReply.NEUTRAL, filter.onMismatch);
  }

  @Test
  public void setOnMatchIgnoresUnknownActions() {
    filter.setOnMatch("DENY");

    filter.setOnMatch("REJECT");
    filter.setOnMatch("accept");
    filter.setOnMatch(null);

    assertEquals(FilterReply.DENY, filter.onMatch);
  }

  @Test
  public void setOnMismatchAcceptsNeutralAcceptAndDeny() {
    filter.setOnMismatch("ACCEPT");
    assertEquals(FilterReply.ACCEPT, filter.onMismatch);
    filter.setOnMismatch("DENY");
    assertEquals(FilterReply.DENY, filter.onMismatch);
    filter.setOnMismatch("NEUTRAL");
    assertEquals(FilterReply.NEUTRAL, filter.onMismatch);
    filter.setOnMismatch("ACCEPT");
    assertEquals(FilterReply.ACCEPT, filter.onMismatch);

    assertEquals(FilterReply.NEUTRAL, filter.onMatch);
  }

  @Test
  public void setOnMismatchIgnoresUnknownActions() {
    filter.setOnMismatch("ACCEPT");

    filter.setOnMismatch("REJECT");
    filter.setOnMismatch("deny");
    filter.setOnMismatch(null);

    assertEquals(FilterReply.ACCEPT, filter.onMismatch);
  }
}
