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

public class FormattingConverterTest {

  /** Converts every event to a fixed (possibly null) string. */
  static class FixedFormattingConverter extends FormattingConverter<Object> {
    private final String value;

    FixedFormattingConverter(String value) {
      this.value = value;
    }

    @Override
    public String convert(Object event) {
      return value;
    }
  }

  private static String write(String value, FormatInfo fi) {
    FixedFormattingConverter c = new FixedFormattingConverter(value);
    if (fi != null) {
      c.setFormattingInfo(fi);
    }
    StringBuilder buf = new StringBuilder("[");
    c.write(buf, new Object());
    return buf.append(']').toString();
  }

  @Test
  public void formattingInfoIsNullUntilSet() {
    FixedFormattingConverter c = new FixedFormattingConverter("x");
    assertNull(c.getFormattingInfo());
    FormatInfo fi = new FormatInfo(1, 2);
    c.setFormattingInfo(fi);
    assertSame(fi, c.getFormattingInfo());
  }

  @Test
  public void settingFormattingInfoTwiceIsRejectedAndKeepsTheFirst() {
    FixedFormattingConverter c = new FixedFormattingConverter("x");
    FormatInfo first = new FormatInfo(1, 2);
    c.setFormattingInfo(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> c.setFormattingInfo(new FormatInfo(3, 4)));
    assertEquals("FormattingInfo has been already set", e.getMessage());
    assertSame(first, c.getFormattingInfo());
  }

  @Test
  public void withoutFormattingInfoConversionIsAppendedAsIs() {
    assertEquals("[abc]", write("abc", null));
    assertEquals("[null]", write(null, null));
  }

  @Test
  public void nullConversionIsPaddedToPositiveMinimum() {
    assertEquals("[   ]", write(null, new FormatInfo(3, 10)));
  }

  @Test
  public void nullConversionIsPaddedToAMinimumOfOne() {
    assertEquals("[ ]", write(null, new FormatInfo(1, 10)));
  }

  @Test
  public void nullConversionWithZeroMinimumAppendsNothing() {
    assertEquals("[]", write(null, new FormatInfo(0, 10)));
  }

  @Test
  public void nullConversionWithDefaultMinimumAppendsNothing() {
    assertEquals("[]", write(null, new FormatInfo()));
  }

  @Test
  public void nullConversionWithNegativeMinimumAppendsNothing() {
    // SpacePadder.spacePad(buf, -5) would append 27 spaces (the low five bits of -5)
    assertEquals("[]", write(null, new FormatInfo(-5, 10)));
  }

  @Test
  public void longConversionIsTruncatedFromTheLeftByDefault() {
    assertEquals("[def]", write("abcdef", new FormatInfo(0, 3, true, true)));
  }

  @Test
  public void longConversionIsTruncatedFromTheRightWhenRequested() {
    assertEquals("[abc]", write("abcdef", new FormatInfo(0, 3, true, false)));
  }

  @Test
  public void shortConversionIsLeftPadded() {
    assertEquals("[  ab]", write("ab", new FormatInfo(4, 10, true, true)));
  }

  @Test
  public void shortConversionIsRightPaddedWhenRequested() {
    assertEquals("[ab  ]", write("ab", new FormatInfo(4, 10, false, true)));
  }

  @Test
  public void conversionWithinBoundsIsAppendedUnchanged() {
    assertEquals("[abcd]", write("abcd", new FormatInfo(4, 4)));
  }
}
