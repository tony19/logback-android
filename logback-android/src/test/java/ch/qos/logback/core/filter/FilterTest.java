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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FilterTest {

  static class DenyAllFilter extends Filter<Object> {
    @Override
    public FilterReply decide(Object event) {
      return FilterReply.DENY;
    }
  }

  private final Filter<Object> filter = new DenyAllFilter();

  @Test
  public void isNotStartedInitially() {
    assertFalse(filter.isStarted());
  }

  @Test
  public void startAndStopToggleStartedState() {
    filter.start();
    assertTrue(filter.isStarted());

    filter.stop();
    assertFalse(filter.isStarted());
  }

  @Test
  public void nameIsNullUntilSet() {
    assertNull(filter.getName());
  }

  @Test
  public void setNameStoresTheName() {
    filter.setName("myFilter");

    assertEquals("myFilter", filter.getName());
  }

  @Test
  public void setNameCanReplaceThePreviousName() {
    filter.setName("first");
    filter.setName("second");

    assertEquals("second", filter.getName());
  }
}
