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
package ch.qos.logback.core.status;

import java.util.Iterator;

import junit.framework.TestCase;

public class StatusBaseTest extends TestCase {

  public void testAddStatus() {
    {
      InfoStatus status = new InfoStatus("testing", this);
      status.add(new ErrorStatus("error", this));
      Iterator<Status> it = status.iterator();
      assertTrue("No status was added", it.hasNext());
      assertTrue("hasChilden method reported wrong result", status
          .hasChildren());
    }
    {
      InfoStatus status = new InfoStatus("testing", this);
      try {
        status.add(null);
        fail("method should have thrown an Exception");
      } catch (NullPointerException ex) {
      }
    }
  }

  public void testRemoveStatus() {
    {
      InfoStatus status = new InfoStatus("testing", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(error);
      boolean result = status.remove(error);
      Iterator<Status> it = status.iterator();
      assertTrue("Remove failed", result);
      assertFalse("No status was removed", it.hasNext());
      assertFalse("hasChilden method reported wrong result", status
          .hasChildren());
    }
    {
      InfoStatus status = new InfoStatus("testing", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(error);
      boolean result = status.remove(null);
      assertFalse("Remove result was not false", result);
    }
  }

  public void testEffectiveLevel() {
    {
      // effective level = 0 level deep
      ErrorStatus status = new ErrorStatus("error", this);
      WarnStatus warn = new WarnStatus("warning", this);
      status.add(warn);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.ERROR);
    }

    {
      // effective level = 1 level deep
      InfoStatus status = new InfoStatus("info", this);
      WarnStatus warn = new WarnStatus("warning", this);
      status.add(warn);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.WARN);
    }

    {
      // effective level = 2 levels deep
      InfoStatus status = new InfoStatus("info", this);
      WarnStatus warn = new WarnStatus("warning", this);
      ErrorStatus error = new ErrorStatus("error", this);
      status.add(warn);
      warn.add(error);
      assertEquals("effective level misevaluated", status.getEffectiveLevel(),
          Status.ERROR);
    }
  }

  public void testRemoveFromStatusWithoutChildrenReturnsFalse() {
    InfoStatus status = new InfoStatus("testing", this);

    assertFalse(status.remove(new ErrorStatus("error", this)));
    assertFalse(status.hasChildren());
  }

  public void testToStringWithoutOriginOmitsOriginPart() {
    InfoStatus status = new InfoStatus("testing", null);

    assertEquals("INFO testing", status.toString());
  }

  public void testToStringWithUnknownLevelOmitsLevelName() {
    InfoStatus status = new InfoStatus("testing", "origin");
    status.level = 42;

    assertEquals(" in origin - testing", status.toString());
  }

  public void testHashCodeCombinesLevelAndMessage() {
    assertEquals(31 * (31 + Status.INFO) + "testing".hashCode(),
        new InfoStatus("testing", this).hashCode());
    assertEquals(31 * (31 + Status.WARN) + "testing".hashCode(),
        new WarnStatus("testing", this).hashCode());
  }

  public void testHashCodeOfNullMessageDependsOnLevelOnly() {
    assertEquals(31 * (31 + Status.ERROR), new ErrorStatus(null, this).hashCode());
  }

  public void testHashCodeIgnoresOriginAndThrowable() {
    InfoStatus a = new InfoStatus("testing", "a");
    InfoStatus b = new InfoStatus("testing", "b", new Exception());

    assertEquals(a.hashCode(), b.hashCode());
  }

  public void testEqualsIsReflexive() {
    InfoStatus status = new InfoStatus("testing", this);

    assertTrue(status.equals(status));
  }

  public void testNotEqualToNull() {
    assertFalse(new InfoStatus("testing", this).equals(null));
  }

  public void testNotEqualToStatusOfAnotherClassWithSameLevelAndMessage() {
    InfoStatus status = new InfoStatus("testing", this);
    InfoStatus subclassStatus = new InfoStatus("testing", this) {
    };

    assertEquals(status.getLevel(), subclassStatus.getLevel());
    assertFalse(status.equals(subclassStatus));
  }

  public void testNotEqualWhenLevelsDiffer() {
    InfoStatus status = new InfoStatus("testing", this);
    InfoStatus other = new InfoStatus("testing", this);
    other.level = Status.WARN;

    assertFalse(status.equals(other));
  }

  public void testEqualWhenLevelAndMessageMatchRegardlessOfOriginAndThrowable() {
    InfoStatus a = new InfoStatus("testing", "a");
    InfoStatus b = new InfoStatus("testing", "b", new Exception());

    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
  }

  public void testNotEqualWhenMessagesDiffer() {
    assertFalse(new InfoStatus("testing", this).equals(new InfoStatus("other", this)));
  }

  public void testEqualWhenBothMessagesAreNull() {
    assertTrue(new InfoStatus(null, this).equals(new InfoStatus(null, this)));
  }

  public void testNotEqualWhenOnlyOneMessageIsNull() {
    InfoStatus withNull = new InfoStatus(null, this);
    InfoStatus withMessage = new InfoStatus("testing", this);

    assertFalse(withNull.equals(withMessage));
    assertFalse(withMessage.equals(withNull));
  }

}
