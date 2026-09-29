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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class NamedConverterTest {

  private static final String FQN = "com.example.pkg.SomeClass";

  private final ILoggingEvent event = mock(ILoggingEvent.class);

  private final NamedConverter converter = new NamedConverter() {
    @Override
    protected String getFullyQualifiedName(ILoggingEvent e) {
      return FQN;
    }
  };

  private void startWithOption(String option) {
    converter.setOptionList(Collections.singletonList(option));
    converter.start();
  }

  @Test
  public void withoutOptionReturnsFullyQualifiedName() {
    converter.start();
    assertNull(converter.abbreviator);
    assertEquals(FQN, converter.convert(event));
  }

  @Test
  public void zeroLengthKeepsOnlyTheClassName() {
    startWithOption("0");
    assertTrue(converter.abbreviator instanceof ClassNameOnlyAbbreviator);
    assertEquals("SomeClass", converter.convert(event));
  }

  @Test
  public void positiveLengthAbbreviatesPackages() {
    startWithOption("17");
    assertTrue(converter.abbreviator instanceof TargetLengthBasedClassNameAbbreviator);
    assertEquals(17, ((TargetLengthBasedClassNameAbbreviator) converter.abbreviator).targetLength);
    assertEquals("c.e.pkg.SomeClass", converter.convert(event));
  }

  @Test
  public void negativeLengthLeavesNameUnabbreviated() {
    startWithOption("-1");
    assertNull(converter.abbreviator);
    assertEquals(FQN, converter.convert(event));
  }

  @Test
  public void nonNumericLengthLeavesNameUnabbreviated() {
    startWithOption("abc");
    assertNull(converter.abbreviator);
    assertEquals(FQN, converter.convert(event));
  }
}
