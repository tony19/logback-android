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
package ch.qos.logback.core.joran.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ElementPathTest {

  @Test
  public void pathsEqualIgnoringCaseHaveEqualHashCodes() {
    ElementPath lower = new ElementPath("configuration/appender");
    ElementPath mixed = new ElementPath("Configuration/APPENDER");

    assertEquals(lower, mixed);
    assertEquals(lower.hashCode(), mixed.hashCode());
  }

  @Test
  public void differentPathsAreNotEqual() {
    assertNotEquals(new ElementPath("a/b"), new ElementPath("a/c"));
    assertNotEquals(new ElementPath("a/b"), new ElementPath("ab"));
  }

  @Test
  public void stringConstructorIgnoresEmptySegments() {
    ElementPath p = new ElementPath("//a///b/");
    assertEquals(2, p.size());
    assertEquals("a", p.get(0));
    assertEquals("b", p.get(1));
    assertEquals(0, new ElementPath("").size());
    assertEquals(0, new ElementPath("/").size());
  }

  @Test
  public void nullStringYieldsEmptyPath() {
    ElementPath p = new ElementPath((String) null);
    assertEquals(0, p.size());
    assertEquals("", p.toString());
  }

  @Test
  public void listConstructorCopiesTheParts() {
    List<String> parts = new ArrayList<String>(Arrays.asList("a", "b"));
    ElementPath p = new ElementPath(parts);
    parts.add("c");

    assertEquals(2, p.size());
    assertEquals("[a][b]", p.toString());
    assertEquals(new ElementPath("a/b"), p);
  }

  @Test
  public void isNeverEqualToNullOrOtherTypes() {
    ElementPath p = new ElementPath("a/b");
    assertFalse(p.equals(null));
    assertFalse(p.equals("a/b"));
  }

  @Test
  public void copyOfPartListIsDetachedFromThePath() {
    ElementPath p = new ElementPath("a/b");
    List<String> copy = p.getCopyOfPartList();
    assertEquals(Arrays.asList("a", "b"), copy);

    copy.clear();
    assertEquals(2, p.size());
  }

  @Test
  public void popRemovesLastPartAndIsANoOpWhenEmpty() {
    ElementPath p = new ElementPath("a/b");
    p.pop();
    assertEquals("[a]", p.toString());
    p.pop();
    assertEquals(0, p.size());
    p.pop();
    assertEquals(0, p.size());
  }

  @Test
  public void peekLastReturnsLastPartOrNullWhenEmpty() {
    ElementPath p = new ElementPath("a/b");
    assertEquals("b", p.peekLast());
    assertNull(new ElementPath().peekLast());
  }
}
