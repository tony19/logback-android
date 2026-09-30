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
package ch.qos.logback.classic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.net.ssl.SSLComponent;
import ch.qos.logback.core.net.ssl.SSLConfiguration;

public class DefaultNestedComponentRulesTest {

  private final DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();

  @Before
  public void setUp() {
    DefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(registry);
  }

  @Test
  public void appendersDefaultTheirLayoutToPatternLayout() {
    assertEquals(PatternLayout.class, registry.findDefaultComponentType(AppenderBase.class, "layout"));
    assertEquals(PatternLayout.class, registry.findDefaultComponentType(UnsynchronizedAppenderBase.class, "layout"));
  }

  @Test
  public void appendersDefaultTheirEncoderToPatternLayoutEncoder() {
    assertEquals(PatternLayoutEncoder.class, registry.findDefaultComponentType(AppenderBase.class, "encoder"));
    assertEquals(PatternLayoutEncoder.class,
        registry.findDefaultComponentType(UnsynchronizedAppenderBase.class, "encoder"));
    // subclasses inherit the default through the registry's superclass walk
    assertEquals(PatternLayoutEncoder.class, registry.findDefaultComponentType(FileAppender.class, "encoder"));
  }

  @Test
  public void sslRulesAreIncluded() {
    assertEquals(SSLConfiguration.class, registry.findDefaultComponentType(SSLComponent.class, "ssl"));
  }

  @Test
  public void unrelatedPropertiesHaveNoDefault() {
    assertNull(registry.findDefaultComponentType(AppenderBase.class, "filter"));
  }

  @Test
  public void utilityClassKeepsItsPublicConstructor() {
    assertNotNull(new DefaultNestedComponentRules());
  }
}
