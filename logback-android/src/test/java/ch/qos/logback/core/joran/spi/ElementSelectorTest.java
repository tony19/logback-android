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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test pattern manipulation code.
 *
 * @author Ceki Gulcu
 */
public class ElementSelectorTest {

  @Test
  public void test1() {
    ElementSelector p = new ElementSelector("a");
    assertEquals(1, p.size());
    assertEquals("a", p.peekLast());
    assertEquals("a", p.get(0));
  }

  @Test
  public void testSuffix() {
    ElementSelector p = new ElementSelector("a/");
    assertEquals(1, p.size());
    assertEquals("a", p.peekLast());
    assertEquals("a", p.get(0));
  }

  @Test
  public void test2() {
    ElementSelector p = new ElementSelector("a/b");
    assertEquals(2, p.size());
    assertEquals("b", p.peekLast());
    assertEquals("a", p.get(0));
    assertEquals("b", p.get(1));
  }

  @Test
  public void test3() {
    ElementSelector p = new ElementSelector("a123/b1234/cvvsdf");
    assertEquals(3, p.size());
    assertEquals("a123", p.get(0));
    assertEquals("b1234", p.get(1));
    assertEquals("cvvsdf", p.get(2));
  }

  @Test
  public void test4() {
    ElementSelector p = new ElementSelector("/a123/b1234/cvvsdf");
    assertEquals(3, p.size());
    assertEquals("a123", p.get(0));
    assertEquals("b1234", p.get(1));
    assertEquals("cvvsdf", p.get(2));
  }

  @Test
  public void test5() {
    ElementSelector p = new ElementSelector("//a");
    assertEquals(1, p.size());
    assertEquals("a", p.get(0));
  }

  @Test
  public void test6() {
    ElementSelector p = new ElementSelector("//a//b");
    assertEquals(2, p.size());
    assertEquals("a", p.get(0));
    assertEquals("b", p.get(1));
  }


  // test tail matching
  @Test
  public void testTailMatch() {
    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("*");
      assertEquals(0, ruleElementSelector.getTailMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a");
      ElementSelector ruleElementSelector = new ElementSelector("*/a");
      assertEquals(1, ruleElementSelector.getTailMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/A");
      ElementSelector ruleElementSelector = new ElementSelector("*/a");
      assertEquals(1, ruleElementSelector.getTailMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a");
      ElementSelector ruleElementSelector = new ElementSelector("*/A");
      assertEquals(1, ruleElementSelector.getTailMatchLength(p));
    }


    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("*/b");
      assertEquals(1, ruleElementSelector.getTailMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/B");
      ElementSelector ruleElementSelector = new ElementSelector("*/b");
      assertEquals(1, ruleElementSelector.getTailMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/b/c");
      ElementSelector ruleElementSelector = new ElementSelector("*/b/c");
      assertEquals(2, ruleElementSelector.getTailMatchLength(p));
    }
  }

  // test prefix matching
  @Test
  public void testPrefixMatch() {
    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("/x/*");
      assertEquals(0, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a");
      ElementSelector ruleElementSelector = new ElementSelector("/x/*");
      assertEquals(0, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("/a/*");
      assertEquals(1, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("/A/*");
      assertEquals(1, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/A/b");
      ElementSelector ruleElementSelector = new ElementSelector("/a/*");
      assertEquals(1, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("/a/b/*");
      assertEquals(2, ruleElementSelector.getPrefixMatchLength(p));
    }

    {
      ElementPath p = new ElementPath("/a/b");
      ElementSelector ruleElementSelector = new ElementSelector("/*");
      assertEquals(0, ruleElementSelector.getPrefixMatchLength(p));
    }
  }

  @Test
  public void noArgAndListConstructors() {
    assertEquals(0, new ElementSelector().size());

    ElementSelector fromList = new ElementSelector(Arrays.asList("a", "b"));
    assertEquals(new ElementSelector("a/b"), fromList);
  }

  @Test
  public void tailMatchLengthIsZeroForNullOrEmptyPaths() {
    ElementSelector selector = new ElementSelector("*/a");
    assertEquals(0, selector.getTailMatchLength(null));
    assertEquals(0, selector.getTailMatchLength(new ElementPath()));
    assertEquals(0, new ElementSelector().getTailMatchLength(new ElementPath("a")));
  }

  @Test
  public void prefixMatchLengthIsZeroForNullOrEmptyPaths() {
    ElementSelector selector = new ElementSelector("a/*");
    assertEquals(0, selector.getPrefixMatchLength(null));
    assertEquals(0, selector.getPrefixMatchLength(new ElementPath()));
    assertEquals(0, new ElementSelector().getPrefixMatchLength(new ElementPath("a")));
  }

  @Test
  public void isContainedInMatchesAContiguousRunOfParts() {
    ElementSelector selector = new ElementSelector("b/c");
    assertTrue(selector.isContainedIn(new ElementPath("a/b/c/d")));
    assertTrue(selector.isContainedIn(new ElementPath("b/c")));
    assertFalse(selector.isContainedIn(new ElementPath("a/b/x/c")));
    assertFalse(selector.isContainedIn(new ElementPath("a/bb/c")));
    assertFalse(selector.isContainedIn(null));
  }

  @Test
  public void equalityIsCaseInsensitiveWithConsistentHashCode() {
    ElementSelector lower = new ElementSelector("a/b");
    ElementSelector mixed = new ElementSelector("A/b");

    assertEquals(lower, mixed);
    assertEquals(lower.hashCode(), mixed.hashCode());
    assertNotEquals(lower, new ElementSelector("a/c"));
    assertNotEquals(lower, new ElementSelector("a/b/c"));
  }

  @Test
  public void isNeverEqualToNullOrAPlainElementPath() {
    ElementSelector selector = new ElementSelector("a/b");
    assertFalse(selector.equals(null));
    assertFalse(selector.equals(new ElementPath("a/b")));
  }

  @Test
  public void fullPathMatchIsCaseInsensitiveAndRequiresSameLength() {
    ElementSelector selector = new ElementSelector("a/b");
    assertTrue(selector.fullPathMatch(new ElementPath("A/B")));
    assertFalse(selector.fullPathMatch(new ElementPath("a/c")));
    assertFalse(selector.fullPathMatch(new ElementPath("a")));
  }
}
