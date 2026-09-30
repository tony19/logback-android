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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.qos.logback.core.pattern.FormatInfo;

public class FormattingNodeTest {

  static FormattingNode node(Object value, FormatInfo formatInfo) {
    FormattingNode n = new FormattingNode(Node.SIMPLE_KEYWORD, value);
    n.setFormatInfo(formatInfo);
    return n;
  }

  @Test
  public void typeOnlyConstructorLeavesValueAndFormatInfoNull() {
    FormattingNode n = new FormattingNode(Node.COMPOSITE_KEYWORD);
    assertEquals(Node.COMPOSITE_KEYWORD, n.getType());
    assertNull(n.getValue());
    assertNull(n.getFormatInfo());
  }

  @Test
  public void setFormatInfoIsReturnedByGetter() {
    FormatInfo fi = new FormatInfo(1, 2);
    assertSame(fi, node("x", fi).getFormatInfo());
  }

  @Test
  public void nodesWithSameFormatInfoAreEqual() {
    assertTrue(node("x", new FormatInfo(1, 2)).equals(node("x", new FormatInfo(1, 2))));
    assertTrue(node("x", null).equals(node("x", null)));
  }

  @Test
  public void nodesDifferingAsPlainNodesAreNotEqual() {
    assertFalse(node("x", new FormatInfo(1, 2)).equals(node("y", new FormatInfo(1, 2))));
  }

  @Test
  public void formattingNodeDoesNotEqualPlainNode() {
    assertFalse(node("x", null).equals(new Node(Node.SIMPLE_KEYWORD, "x")));
  }

  @Test
  public void nodesWithDifferentFormatInfoAreNotEqual() {
    assertFalse(node("x", new FormatInfo(1, 2)).equals(node("x", new FormatInfo(1, 3))));
    assertFalse(node("x", new FormatInfo(1, 2)).equals(node("x", null)));
    assertFalse(node("x", null).equals(node("x", new FormatInfo(1, 2))));
  }

  @Test
  public void hashCodeCombinesNodeHashAndFormatInfo() {
    int nodeHash = new Node(Node.SIMPLE_KEYWORD, "x").hashCode();
    FormatInfo fi = new FormatInfo(1, 2);
    assertEquals(31 * nodeHash + fi.hashCode(), node("x", fi).hashCode());
    assertEquals(31 * nodeHash, node("x", null).hashCode());
  }
}
