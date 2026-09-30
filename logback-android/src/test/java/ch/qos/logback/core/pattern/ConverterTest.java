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
package ch.qos.logback.core.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ConverterTest {

  @Test
  public void writeAppendsTheConversion() {
    StringBuilder buf = new StringBuilder("x");
    new LiteralConverter<Object>("abc").write(buf, new Object());
    assertEquals("xabc", buf.toString());
  }

  @Test
  public void nextIsNullUntilSet() {
    Converter<Object> first = new LiteralConverter<Object>("a");
    assertNull(first.getNext());

    Converter<Object> second = new LiteralConverter<Object>("b");
    first.setNext(second);
    assertSame(second, first.getNext());
  }

  @Test
  public void settingNextTwiceIsRejectedAndKeepsTheFirstNext() {
    Converter<Object> first = new LiteralConverter<Object>("a");
    Converter<Object> second = new LiteralConverter<Object>("b");
    Converter<Object> third = new LiteralConverter<Object>("c");
    first.setNext(second);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> first.setNext(third));
    assertEquals("Next converter has been already set", e.getMessage());
    assertSame(second, first.getNext());
  }
}
