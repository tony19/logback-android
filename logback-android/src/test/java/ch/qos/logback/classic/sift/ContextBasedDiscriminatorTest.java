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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Collections;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;

public class ContextBasedDiscriminatorTest {

  static final String DEFAULT_VAL = "DEFAULT_VAL";

  ContextBasedDiscriminator discriminator = new ContextBasedDiscriminator();
  LoggerContext context = new LoggerContext();
  Logger logger = context.getLogger(this.getClass());

  @Test
  public void discriminatingValueIsTheNameOfTheEventContext() {
    context.setName("contextBasedDiscriminatorTest");
    discriminator.setDefaultValue(DEFAULT_VAL);
    LoggingEvent event = new LoggingEvent("a", logger, Level.DEBUG, "", null, null);

    assertEquals("contextBasedDiscriminatorTest", discriminator.getDiscriminatingValue(event));
  }

  @Test
  public void discriminatingValueIsTheDefaultValueWhenTheContextHasNoName() {
    discriminator.setDefaultValue(DEFAULT_VAL);
    LoggingEvent event = new LoggingEvent("a", logger, Level.DEBUG, "", null, null);
    event.setLoggerContextRemoteView(
        new LoggerContextVO(null, Collections.<String, String>emptyMap(), 0L));

    assertEquals(DEFAULT_VAL, discriminator.getDiscriminatingValue(event));
  }

  @Test
  public void defaultValueIsNullUnlessSet() {
    assertNull(discriminator.getDefaultValue());

    discriminator.setDefaultValue(DEFAULT_VAL);

    assertEquals(DEFAULT_VAL, discriminator.getDefaultValue());
  }

  @Test
  public void keyIsFixedToContextName() {
    assertEquals("contextName", discriminator.getKey());

    UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
        () -> discriminator.setKey("other"));

    assertEquals("Key cannot be set. Using fixed key contextName", e.getMessage());
    assertEquals("contextName", discriminator.getKey());
  }
}
