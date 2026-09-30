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

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class DateTokenConverterTest {

  private DateTokenConverter<Object> converter;

  @Before
  public void setUp() {
    converter = new DateTokenConverter<Object>();
    converter.setOptionList(Arrays.asList("yyyy-MM-dd", "GMT"));
    converter.start();
  }

  @Test
  public void convertsDate() {
    assertEquals("1970-01-02", converter.convert((Object) new Date(24L * 60 * 60 * 1000)));
  }

  @Test
  public void convertingNullIsForbidden() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> converter.convert((Object) null));

    assertEquals("Null argument forbidden", e.getMessage());
  }

  @Test
  public void convertingNonDateIsRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> converter.convert((Object) Integer.valueOf(3)));

    assertEquals("Cannot convert 3 of typejava.lang.Integer", e.getMessage());
  }
}
