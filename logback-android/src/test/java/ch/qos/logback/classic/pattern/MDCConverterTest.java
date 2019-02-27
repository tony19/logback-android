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
package ch.qos.logback.classic.pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import ch.qos.logback.core.testUtil.RandomUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class MDCConverterTest {

  LoggerContext lc;
  MDCConverter converter;
  int diff = RandomUtil.getPositiveInt();

  @Before
  public void setUp() throws Exception {
    lc = new LoggerContext();
    converter = new MDCConverter();
    converter.start();
    MDC.clear();
  }

  @After
  public void tearDown() throws Exception {
    lc = null;
    converter.stop();
    converter = null;
    MDC.clear();
  }

  @Test
  public void testConvertWithOneEntry() {
    String k = "MDCConverterTest_k"+diff;
    String v = "MDCConverterTest_v"+diff;

    MDC.put(k, v);
    ILoggingEvent le = createLoggingEvent();
    String result = converter.convert(le);
    assertEquals(k+"="+v, result);
  }

  @Test
  public void testConvertWithMultipleEntries() {
    MDC.put("testKey", "testValue");
    MDC.put("testKey2", "testValue2");
    ILoggingEvent le = createLoggingEvent();
    String result = converter.convert(le);
    boolean isConform = result.matches("testKey2?=testValue2?, testKey2?=testValue2?");
    assertTrue(result + " is not conform", isConform);
  }

  private ILoggingEvent createLoggingEvent() {
    return new LoggingEvent(this.getClass().getName(), lc
        .getLogger(Logger.ROOT_LOGGER_NAME), Level.DEBUG, "test message", null,
        null);
  }
}
