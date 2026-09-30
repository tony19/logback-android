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
package ch.qos.logback.classic.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventArgUtilTest {

  @Test
  public void noThrowableIsExtractedFromNullOrEmptyArray() {
    assertNull(EventArgUtil.extractThrowable(null));
    assertNull(EventArgUtil.extractThrowable(new Object[0]));
  }

  @Test
  public void noThrowableIsExtractedWhenLastArgumentIsNotAThrowable() {
    Exception notLast = new Exception("not last");
    assertNull(EventArgUtil.extractThrowable(new Object[] {notLast, "a"}));
  }

  @Test
  public void lastArgumentIsExtractedWhenItIsAThrowable() {
    Exception last = new Exception("last");
    assertSame(last, EventArgUtil.extractThrowable(new Object[] {"a", last}));
  }

  @Test
  public void trimmedCopyDropsTheLastArgument() {
    Object[] args = new Object[] {"a", 2, new Exception("last")};

    Object[] trimmed = EventArgUtil.trimmedCopy(args);

    assertArrayEquals(new Object[] {"a", 2}, trimmed);
    assertEquals("the input array is left untouched", 3, args.length);
  }

  @Test
  public void trimmedCopyRejectsNullArray() {
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> EventArgUtil.trimmedCopy(null));
    assertEquals("non-sensical empty or null argument array", e.getMessage());
  }

  @Test
  public void trimmedCopyRejectsEmptyArray() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> EventArgUtil.trimmedCopy(new Object[0]));
    assertEquals("non-sensical empty or null argument array", e.getMessage());
  }

  @Test
  public void extractionSucceedsOnlyForNonNullThrowable() {
    assertTrue(EventArgUtil.successfulExtraction(new Exception()));
    assertFalse(EventArgUtil.successfulExtraction(null));
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class only has static members, but its implicit public constructor is API
    assertNotNull(new EventArgUtil());
  }
}
