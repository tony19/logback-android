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
package ch.qos.logback.classic.sift;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.testUtil.RandomUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.MDC;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class MDCBasedDiscriminatorTest {

  static String DEFAULT_VAL = "DEFAULT_VAL";

  MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
  LoggerContext context = new LoggerContext();
  Logger logger = context.getLogger(this.getClass());

  int diff = RandomUtil.getPositiveInt();
  String key = "MDCBasedDiscriminatorTest_key" + diff;
  String value = "MDCBasedDiscriminatorTest_val" + diff;
  LoggingEvent event;

  @Before
  public void setUp() {
    MDC.clear();
    discriminator.setContext(context);
    discriminator.setKey(key);
    discriminator.setDefaultValue(DEFAULT_VAL);
    discriminator.start();
    assertTrue(discriminator.isStarted());
  }

  @After
  public void tearDown() {
    MDC.clear();
  }

  @Test
  public void smoke() {
    MDC.put(key, value);
    event = new LoggingEvent("a", logger, Level.DEBUG, "", null, null);

    String discriminatorValue = discriminator.getDiscriminatingValue(event);
    assertEquals(value, discriminatorValue);
  }

  @Test
  public void nullMDC() {
    event = new LoggingEvent("a", logger, Level.DEBUG, "", null, null);
    assertTrue(event.getMDCPropertyMap().isEmpty());
    String discriminatorValue = discriminator.getDiscriminatingValue(event);
    assertEquals(DEFAULT_VAL, discriminatorValue);
  }

  // LBCLASSIC-213: events, e.g. deserialized ones, may carry no MDC map at all
  @Test
  public void eventWithoutMDCMapYieldsTheDefaultValue() {
    ILoggingEvent eventWithoutMDCMap = mock(ILoggingEvent.class);
    when(eventWithoutMDCMap.getMDCPropertyMap()).thenReturn(null);

    assertEquals(DEFAULT_VAL, discriminator.getDiscriminatingValue(eventWithoutMDCMap));
  }

  @Test
  public void defaultValueIsExposed() {
    assertEquals(DEFAULT_VAL, discriminator.getDefaultValue());
  }

  @Test
  public void startFailsWithoutKey() {
    MDCBasedDiscriminator withoutKey = new MDCBasedDiscriminator();
    withoutKey.setContext(context);
    withoutKey.setDefaultValue(DEFAULT_VAL);

    withoutKey.start();

    assertFalse(withoutKey.isStarted());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("The \"Key\" property must be set", statuses.get(0).getMessage());
  }

  @Test
  public void startFailsWithoutDefaultValue() {
    MDCBasedDiscriminator withoutDefaultValue = new MDCBasedDiscriminator();
    withoutDefaultValue.setContext(context);
    withoutDefaultValue.setKey(key);

    withoutDefaultValue.start();

    assertFalse(withoutDefaultValue.isStarted());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("The \"DefaultValue\" property must be set", statuses.get(0).getMessage());
  }

  @Test
  public void startReportsBothMissingProperties() {
    MDCBasedDiscriminator unconfigured = new MDCBasedDiscriminator();
    unconfigured.setContext(context);

    unconfigured.start();

    assertFalse(unconfigured.isStarted());
    assertEquals(2, context.getStatusManager().getCount());
  }
}
