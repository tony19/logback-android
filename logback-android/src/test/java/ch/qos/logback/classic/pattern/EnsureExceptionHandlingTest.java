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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;

public class EnsureExceptionHandlingTest {

  private final LoggerContext lc = new LoggerContext();
  private final EnsureExceptionHandling processor = new EnsureExceptionHandling();

  @Test
  public void rejectsEmptyChain() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> processor.process(lc, null));
    assertEquals("cannot process empty chain", e.getMessage());
  }

  @Test
  public void appendsThrowableConverterToChainWithoutOne() {
    lc.setPackagingDataEnabled(false);
    Converter<ILoggingEvent> head = new LiteralConverter<ILoggingEvent>("a");
    Converter<ILoggingEvent> tail = new LiteralConverter<ILoggingEvent>("b");
    head.setNext(tail);

    processor.process(lc, head);

    assertSame(tail, head.getNext());
    assertEquals(ThrowableProxyConverter.class, tail.getNext().getClass());
    assertNull(tail.getNext().getNext());
  }

  @Test
  public void appendsExtendedThrowableConverterWhenPackagingDataIsEnabled() {
    lc.setPackagingDataEnabled(true);
    Converter<ILoggingEvent> head = new LiteralConverter<ILoggingEvent>("a");

    processor.process(lc, head);

    assertEquals(ExtendedThrowableProxyConverter.class, head.getNext().getClass());
  }

  @Test
  public void leavesChainThatHandlesThrowablesUntouched() {
    Converter<ILoggingEvent> head = new LiteralConverter<ILoggingEvent>("a");
    ThrowableProxyConverter throwableConverter = new ThrowableProxyConverter();
    head.setNext(throwableConverter);
    throwableConverter.setNext(new LiteralConverter<ILoggingEvent>("c"));
    Converter<ILoggingEvent> tail = throwableConverter.getNext();

    processor.process(lc, head);

    assertSame(throwableConverter, head.getNext());
    assertSame(tail, throwableConverter.getNext());
    assertNull(tail.getNext());
  }
}
