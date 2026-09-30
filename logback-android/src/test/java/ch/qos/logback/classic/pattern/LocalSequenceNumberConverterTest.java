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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LocalSequenceNumberConverterTest {

  private final ILoggingEvent event = mock(ILoggingEvent.class);

  @Test
  public void sequenceStartsAtCreationTimeInMillis() {
    long before = System.currentTimeMillis();
    LocalSequenceNumberConverter converter = new LocalSequenceNumberConverter();
    long after = System.currentTimeMillis();

    long first = Long.parseLong(converter.convert(event));

    assertTrue(first + " < " + before, first >= before);
    assertTrue(first + " > " + after, first <= after);
  }

  @Test
  public void eachConversionYieldsTheNextNumber() {
    LocalSequenceNumberConverter converter = new LocalSequenceNumberConverter();
    converter.sequenceNumber.set(41);

    assertEquals("41", converter.convert(event));
    assertEquals("42", converter.convert(event));
    assertEquals("43", converter.convert(event));
  }

  @Test
  public void instancesKeepIndependentSequences() {
    LocalSequenceNumberConverter first = new LocalSequenceNumberConverter();
    LocalSequenceNumberConverter second = new LocalSequenceNumberConverter();
    first.sequenceNumber.set(10);
    second.sequenceNumber.set(100);

    assertEquals("10", first.convert(event));
    assertEquals("100", second.convert(event));
    assertEquals("11", first.convert(event));
  }
}
