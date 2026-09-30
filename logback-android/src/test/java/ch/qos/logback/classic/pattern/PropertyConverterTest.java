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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.testUtil.RandomUtil;

public class PropertyConverterTest {

  private final String key = "PropertyConverterTest.key." + RandomUtil.getPositiveInt();
  private final LoggerContext lc = new LoggerContext();
  private final PropertyConverter converter = new PropertyConverter();

  @After
  public void tearDown() {
    System.clearProperty(key);
  }

  private ILoggingEvent event() {
    Logger logger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    return new LoggingEvent(getClass().getName(), logger, Level.INFO, "msg", null, null);
  }

  private void startWithKey() {
    converter.setContext(lc);
    converter.setOptionList(Collections.singletonList(key));
    converter.start();
  }

  @Test
  public void withoutKeyStaysStoppedAndSaysSo() {
    converter.setContext(lc);
    converter.start();

    assertFalse(converter.isStarted());
    assertNull(converter.key);
    assertEquals("Property_HAS_NO_KEY", converter.convert(event()));
  }

  @Test
  public void contextPropertyTakesPrecedenceOverSystemProperty() {
    startWithKey();
    assertTrue(converter.isStarted());
    lc.putProperty(key, "fromContext");
    System.setProperty(key, "fromSystem");

    assertEquals("fromContext", converter.convert(event()));
  }

  @Test
  public void fallsBackToSystemPropertyWhenContextLacksKey() {
    startWithKey();
    System.setProperty(key, "fromSystem");

    assertEquals("fromSystem", converter.convert(event()));
  }

  @Test
  public void convertsToNullWhenPropertyIsDefinedNowhere() {
    startWithKey();

    assertNull(converter.convert(event()));
  }
}
