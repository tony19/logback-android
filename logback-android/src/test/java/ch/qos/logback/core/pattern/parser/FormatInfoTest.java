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
package ch.qos.logback.core.pattern.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import ch.qos.logback.core.pattern.FormatInfo;


public class FormatInfoTest  {

  @Test
  public void testEndingInDot() {
    try {
      FormatInfo.valueOf("45.");
      fail("45. is not a valid format info string");
    } catch (IllegalArgumentException iae) {
      // OK
    }
  }

  @Test
  public void testBasic() {
    {
      FormatInfo fi = FormatInfo.valueOf("45");
      FormatInfo witness = new FormatInfo();
      witness.setMin(45);
      assertEquals(witness, fi);
    }

    {
      FormatInfo fi = FormatInfo.valueOf("4.5");
      FormatInfo witness = new FormatInfo();
      witness.setMin(4);
      witness.setMax(5);
      assertEquals(witness, fi);
    }
  }

  @Test
  public void testRightPad() {
    {
      FormatInfo fi = FormatInfo.valueOf("-40");
      FormatInfo witness = new FormatInfo();
      witness.setMin(40);
      witness.setLeftPad(false);
      assertEquals(witness, fi);
    }

    {
      FormatInfo fi = FormatInfo.valueOf("-12.5");
      FormatInfo witness = new FormatInfo();
      witness.setMin(12);
      witness.setMax(5);
      witness.setLeftPad(false);
      assertEquals(witness, fi);
    }

    {
      FormatInfo fi = FormatInfo.valueOf("-14.-5");
      FormatInfo witness = new FormatInfo();
      witness.setMin(14);
      witness.setMax(5);
      witness.setLeftPad(false);
      witness.setLeftTruncate(false);
      assertEquals(witness, fi);
    }
  }

  @Test
  public void testMinOnly() {
    {
      FormatInfo fi = FormatInfo.valueOf("49");
      FormatInfo witness = new FormatInfo();
      witness.setMin(49);
      assertEquals(witness, fi);
    }

    {
      FormatInfo fi = FormatInfo.valueOf("-587");
      FormatInfo witness = new FormatInfo();
      witness.setMin(587);
      witness.setLeftPad(false);
      assertEquals(witness, fi);
    }

  }

  @Test
  public void testMaxOnly() {
    {
      FormatInfo fi = FormatInfo.valueOf(".49");
      FormatInfo witness = new FormatInfo();
      witness.setMax(49);
      assertEquals(witness, fi);
    }

    {
      FormatInfo fi = FormatInfo.valueOf(".-5");
      FormatInfo witness = new FormatInfo();
      witness.setMax(5);
      witness.setLeftTruncate(false);
      assertEquals(witness, fi);
    }
  }

  @Test
  public void emptyStringKeepsAllDefaults() {
    FormatInfo fi = FormatInfo.valueOf("");
    assertEquals(new FormatInfo(), fi);
    assertEquals(Integer.MIN_VALUE, fi.getMin());
    assertEquals(Integer.MAX_VALUE, fi.getMax());
    assertTrue(fi.isLeftPad());
    assertTrue(fi.isLeftTruncate());
  }

  @Test
  public void emptyMinPartBeforeDotKeepsDefaultMinAndPadding() {
    FormatInfo fi = FormatInfo.valueOf(".-7");
    assertEquals(Integer.MIN_VALUE, fi.getMin());
    assertTrue(fi.isLeftPad());
    assertEquals(7, fi.getMax());
    assertFalse(fi.isLeftTruncate());
  }

  @Test
  public void withoutDotMaxKeepsDefaultAndTruncatesLeft() {
    FormatInfo fi = FormatInfo.valueOf("-3");
    assertEquals(3, fi.getMin());
    assertFalse(fi.isLeftPad());
    assertEquals(Integer.MAX_VALUE, fi.getMax());
    assertTrue(fi.isLeftTruncate());
  }

  @Test
  public void loneDotIsRejectedAsEndingInDot() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FormatInfo.valueOf("."));
    assertEquals("Formatting string [.] should not end with '.'", e.getMessage());
  }

  @Test
  public void numberEndingInDotIsRejectedWithMessage() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FormatInfo.valueOf("-4."));
    assertEquals("Formatting string [-4.] should not end with '.'", e.getMessage());
  }

  @Test
  public void nullStringIsRejected() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> FormatInfo.valueOf(null));
    assertEquals("Argument cannot be null", e.getMessage());
  }

  @Test
  public void twoArgConstructorKeepsLeftPadAndLeftTruncate() {
    FormatInfo fi = new FormatInfo(3, 9);
    assertEquals(new FormatInfo(3, 9, true, true), fi);
  }

  @Test
  public void equalsIsReflexive() {
    FormatInfo fi = new FormatInfo(1, 2, false, false);
    assertTrue(fi.equals(fi));
  }

  @Test
  public void equalsRejectsNullAndOtherTypes() {
    FormatInfo fi = new FormatInfo(1, 2, true, true);
    assertFalse(fi.equals(null));
    assertFalse(fi.equals("FormatInfo(1, 2, true, true)"));
  }

  @Test
  public void equalsComparesEveryField() {
    FormatInfo fi = new FormatInfo(1, 2, true, true);
    assertTrue(fi.equals(new FormatInfo(1, 2, true, true)));
    assertFalse(fi.equals(new FormatInfo(9, 2, true, true)));
    assertFalse(fi.equals(new FormatInfo(1, 9, true, true)));
    assertFalse(fi.equals(new FormatInfo(1, 2, false, true)));
    assertFalse(fi.equals(new FormatInfo(1, 2, true, false)));
  }

  @Test
  public void hashCodeCombinesEveryField() {
    // ((min * 31 + max) * 31 + leftPad) * 31 + leftTruncate
    assertEquals(31745, new FormatInfo(1, 2, true, true).hashCode());
    assertEquals(31744, new FormatInfo(1, 2, true, false).hashCode());
    assertEquals(31714, new FormatInfo(1, 2, false, true).hashCode());
    assertEquals(31713, new FormatInfo(1, 2, false, false).hashCode());
    assertEquals(new FormatInfo(4, 5).hashCode(), FormatInfo.valueOf("4.5").hashCode());
    assertNotEquals(new FormatInfo(4, 5).hashCode(), new FormatInfo(5, 4).hashCode());
  }

  @Test
  public void toStringListsEveryField() {
    assertEquals("FormatInfo(1, 2, true, false)", new FormatInfo(1, 2, true, false).toString());
    assertEquals("FormatInfo(3, 4, false, true)", FormatInfo.valueOf("-3.4").toString());
  }
}
