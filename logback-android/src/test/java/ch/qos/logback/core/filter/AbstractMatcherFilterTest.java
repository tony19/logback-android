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
package ch.qos.logback.core.filter;

import org.junit.Test;

import ch.qos.logback.core.spi.FilterReply;

import static org.junit.Assert.assertEquals;

public class AbstractMatcherFilterTest {

  static class MatchesTrueFilter extends AbstractMatcherFilter<Boolean> {
    @Override
    public FilterReply decide(Boolean event) {
      return event ? onMatch : onMismatch;
    }
  }

  private final MatchesTrueFilter filter = new MatchesTrueFilter();

  @Test
  public void onMatchAndOnMismatchDefaultToNeutral() {
    assertEquals(FilterReply.NEUTRAL, filter.getOnMatch());
    assertEquals(FilterReply.NEUTRAL, filter.getOnMismatch());
  }

  @Test
  public void setOnMatchIsReturnedByGetterAndUsedForMatches() {
    filter.setOnMatch(FilterReply.ACCEPT);

    assertEquals(FilterReply.ACCEPT, filter.getOnMatch());
    assertEquals(FilterReply.NEUTRAL, filter.getOnMismatch());
    assertEquals(FilterReply.ACCEPT, filter.decide(true));
  }

  @Test
  public void setOnMismatchIsReturnedByGetterAndUsedForMismatches() {
    filter.setOnMismatch(FilterReply.DENY);

    assertEquals(FilterReply.DENY, filter.getOnMismatch());
    assertEquals(FilterReply.NEUTRAL, filter.getOnMatch());
    assertEquals(FilterReply.DENY, filter.decide(false));
  }
}
