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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MethodOfCallerConverterTest {

  private final MethodOfCallerConverter converter = new MethodOfCallerConverter();

  private static ILoggingEvent eventWithCallerData(StackTraceElement... callerData) {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getCallerData()).thenReturn(callerData);
    return event;
  }

  @Test
  public void convertsToMethodOfFirstCallerFrame() {
    ILoggingEvent event = eventWithCallerData(
        new StackTraceElement("com.example.First", "firstMethod", "First.java", 1),
        new StackTraceElement("com.example.Second", "secondMethod", "Second.java", 2));
    assertEquals("firstMethod", converter.convert(event));
  }

  @Test
  public void convertsToNotAvailableWithoutCallerData() {
    assertEquals(CallerData.NA, converter.convert(eventWithCallerData((StackTraceElement[]) null)));
  }

  @Test
  public void convertsToNotAvailableWithEmptyCallerData() {
    assertEquals(CallerData.NA, converter.convert(eventWithCallerData()));
  }
}
