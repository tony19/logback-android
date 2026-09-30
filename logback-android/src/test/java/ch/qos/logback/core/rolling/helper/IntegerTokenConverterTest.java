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
package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class IntegerTokenConverterTest {

  private final IntegerTokenConverter converter = new IntegerTokenConverter();

  @Test
  public void convertsInteger() {
    assertEquals("42", converter.convert((Object) Integer.valueOf(42)));
  }

  @Test
  public void convertingNullIsForbidden() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> converter.convert((Object) null));

    assertEquals("Null argument forbidden", e.getMessage());
  }

  @Test
  public void convertingNonIntegerIsRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> converter.convert((Object) "42"));

    assertEquals("Cannot convert 42 of typejava.lang.String", e.getMessage());
  }
}
