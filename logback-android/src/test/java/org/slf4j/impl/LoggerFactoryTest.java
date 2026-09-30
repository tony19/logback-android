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
package org.slf4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.slf4j.Logger;

public class LoggerFactoryTest {

  private final LoggerFactory factory = new LoggerFactory();

  @Test
  public void createsLogbackLoggersFromItsOwnContext() {
    Logger logger = factory.getLogger("a.b.C");

    assertTrue(logger instanceof ch.qos.logback.classic.Logger);
    assertEquals("a.b.C", logger.getName());
    assertSame(factory.loggerContext,
        ((ch.qos.logback.classic.Logger) logger).getLoggerContext());
  }

  @Test
  public void returnsTheCachedLoggerForARepeatedName() {
    Logger first = factory.getLogger("repeated");

    assertSame(first, factory.getLogger("repeated"));
    assertSame(first, factory.loggerMap.get("repeated"));
  }

  @Test
  public void cachedLoggerIsReturnedWithoutCreatingOneInTheContext() {
    Logger cached = new LoggerFactory().getLogger("preset");
    factory.loggerMap.put("preset", cached);

    assertSame(cached, factory.getLogger("preset"));
    assertNull(factory.loggerContext.exists("preset"));
  }

  @Test
  public void distinctNamesGetDistinctLoggers() {
    assertNotSame(factory.getLogger("one"), factory.getLogger("two"));
    assertEquals(2, factory.loggerMap.size());
  }

  @Test
  public void loggerCachedConcurrentlyByAnotherThreadWinsOverTheNewOne() {
    final Logger cachedByOtherThread = new LoggerFactory().getLogger("raced");
    // simulate another thread caching a logger between the lookup and putIfAbsent
    factory.loggerMap = new ConcurrentHashMap<String, Logger>() {
      @Override
      public Logger get(Object key) {
        Logger existing = super.get(key);
        if (existing == null) {
          put((String) key, cachedByOtherThread);
        }
        return existing;
      }
    };

    assertSame(cachedByOtherThread, factory.getLogger("raced"));
    assertSame(cachedByOtherThread, factory.loggerMap.get("raced"));
  }
}
