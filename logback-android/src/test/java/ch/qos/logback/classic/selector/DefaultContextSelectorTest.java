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
package ch.qos.logback.classic.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

public class DefaultContextSelectorTest {

  private final LoggerContext context = new LoggerContext();
  private DefaultContextSelector selector;

  @Before
  public void setUp() {
    context.setName("ctx");
    selector = new DefaultContextSelector(context);
  }

  @Test
  public void alwaysSelectsTheDefaultContext() {
    assertSame(context, selector.getDefaultLoggerContext());
    assertSame(context, selector.getLoggerContext());
  }

  @Test
  public void detachingAnyNameReturnsTheDefaultContext() {
    assertSame(context, selector.detachLoggerContext("ctx"));
    assertSame(context, selector.detachLoggerContext("other"));
    // detaching does not remove the default context
    assertSame(context, selector.getLoggerContext());
  }

  @Test
  public void contextNamesListOnlyTheDefaultContext() {
    assertEquals(Collections.singletonList("ctx"), selector.getContextNames());
  }

  @Test
  public void lookupByNameFindsOnlyTheDefaultContext() {
    assertSame(context, selector.getLoggerContext("ctx"));
    assertNull(selector.getLoggerContext("other"));
    assertNull(selector.getLoggerContext(null));
  }
}
