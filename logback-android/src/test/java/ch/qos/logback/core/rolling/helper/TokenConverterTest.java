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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class TokenConverterTest {

  @Test
  public void typeIsSetAtConstructionAndCanBeChanged() {
    TokenConverter converter = new TokenConverter(TokenConverter.INTEGER);
    assertEquals(TokenConverter.INTEGER, converter.getType());

    converter.setType(TokenConverter.IDENTITY);

    assertEquals(TokenConverter.IDENTITY, converter.getType());
  }

  @Test
  public void convertersCanBeChained() {
    TokenConverter head = new TokenConverter(TokenConverter.IDENTITY);
    TokenConverter next = new TokenConverter(TokenConverter.DATE);
    assertNull(head.getNext());

    head.setNext(next);

    assertSame(next, head.getNext());
    assertNull(next.getNext());
  }
}
