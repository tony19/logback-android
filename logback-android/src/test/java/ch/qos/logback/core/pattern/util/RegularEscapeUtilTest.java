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
package ch.qos.logback.core.pattern.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class RegularEscapeUtilTest {

  private final RegularEscapeUtil util = new RegularEscapeUtil();

  private String escape(String escapeChars, char next) {
    StringBuffer buf = new StringBuffer("[");
    util.escape(escapeChars, buf, next, 0);
    return buf.append(']').toString();
  }

  @Test
  public void escapeCharsAreKeptLiterally() {
    assertEquals("[%]", escape("%)", '%'));
    assertEquals("[)]", escape("%)", ')'));
  }

  @Test
  public void escapedUnderscoreIsSwallowed() {
    assertEquals("[]", escape("%", '_'));
  }

  @Test
  public void escapedBackslashIsKept() {
    assertEquals("[\\]", escape("%", '\\'));
  }

  @Test
  public void escapedControlLettersBecomeControlCharacters() {
    assertEquals("[\t]", escape("%", 't'));
    assertEquals("[\r]", escape("%", 'r'));
    assertEquals("[\n]", escape("%", 'n'));
  }

  @Test
  public void escapeCharsTakePrecedenceOverControlLetters() {
    assertEquals("[n]", escape("n", 'n'));
  }

  @Test
  public void illegalEscapeIsRejectedWithTheAllowedEscapesListed() {
    StringBuffer buf = new StringBuffer();
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> util.escape("%)", buf, 'x', 7));
    assertEquals("Illegal char 'x at column 7. Only \\\\, \\_, \\%, \\), \\t, \\n, \\r combinations"
        + " are allowed as escape characters.", e.getMessage());
    assertEquals("", buf.toString());
  }

  @Test
  public void illegalEscapeWithoutEscapeCharsListsOnlyTheBuiltInEscapes() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> util.escape("", new StringBuffer(), 'q', 3));
    assertEquals("Illegal char 'q at column 3. Only \\\\, \\_, \\t, \\n, \\r combinations"
        + " are allowed as escape characters.", e.getMessage());
  }

  @Test
  public void formatEscapeCharsForListingPrefixesEachCharWithCommaAndBackslash() {
    assertEquals("", util.formatEscapeCharsForListing(""));
    assertEquals(", \\%", util.formatEscapeCharsForListing("%"));
    assertEquals(", \\%, \\), \\}", util.formatEscapeCharsForListing("%)}"));
  }

  @Test
  public void basicEscapeLeavesUnescapedTextUnchanged() {
    assertEquals("", RegularEscapeUtil.basicEscape(""));
    assertEquals("plain text", RegularEscapeUtil.basicEscape("plain text"));
  }

  @Test
  public void basicEscapeTranslatesControlEscapes() {
    assertEquals("a\nb", RegularEscapeUtil.basicEscape("a\\nb"));
    assertEquals("a\rb", RegularEscapeUtil.basicEscape("a\\rb"));
    assertEquals("a\tb", RegularEscapeUtil.basicEscape("a\\tb"));
    assertEquals("a\fb", RegularEscapeUtil.basicEscape("a\\fb"));
  }

  @Test
  public void basicEscapeDropsTheBackslashBeforeOtherChars() {
    assertEquals("a.b", RegularEscapeUtil.basicEscape("a\\.b"));
    assertEquals("a\\b", RegularEscapeUtil.basicEscape("a\\\\b"));
    assertEquals("xy", RegularEscapeUtil.basicEscape("\\x\\y"));
  }
}
