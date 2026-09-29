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

import java.util.Arrays;

import org.junit.Test;

public class CompositeNodeTest {

  static CompositeNode composite(String keyword, Node child) {
    CompositeNode n = new CompositeNode(keyword);
    n.setChildNode(child);
    return n;
  }

  static Node literal(String value) {
    return new Node(Node.LITERAL, value);
  }

  @Test
  public void compositeNodeHasCompositeKeywordType() {
    Node child = literal("a");
    CompositeNode n = composite("k", child);
    assertEquals(Node.COMPOSITE_KEYWORD, n.getType());
    assertEquals("k", n.getValue());
    assertSame(child, n.getChildNode());
    assertNull(new CompositeNode("k").getChildNode());
  }

  @Test
  public void nodesWithEqualChildrenAreEqual() {
    assertTrue(composite("k", literal("a")).equals(composite("k", literal("a"))));
    assertTrue(composite("k", null).equals(composite("k", null)));
  }

  @Test
  public void nodesDifferingAsKeywordNodesAreNotEqual() {
    CompositeNode withOptions = composite("k", null);
    withOptions.setOptions(Arrays.asList("o"));
    assertFalse(withOptions.equals(composite("k", null)));
  }

  @Test
  public void compositeNodeDoesNotEqualKeywordNodeOfSameType() {
    SimpleKeywordNode keywordNode = new SimpleKeywordNode(Node.COMPOSITE_KEYWORD, "k");
    assertFalse(composite("k", null).equals(keywordNode));
  }

  @Test
  public void nodesWithDifferentChildrenAreNotEqual() {
    assertFalse(composite("k", literal("a")).equals(composite("k", literal("b"))));
    assertFalse(composite("k", literal("a")).equals(composite("k", null)));
    assertFalse(composite("k", null).equals(composite("k", literal("a"))));
  }

  @Test
  public void hashCodeIsKeywordNodeHashCode() {
    SimpleKeywordNode keywordNode = new SimpleKeywordNode(Node.COMPOSITE_KEYWORD, "k");
    assertEquals(keywordNode.hashCode(), composite("k", literal("a")).hashCode());
  }

  @Test
  public void toStringShowsChild() {
    assertEquals("CompositeNode(LITERAL(a))", composite("k", literal("a")).toString());
  }

  @Test
  public void toStringWithoutChild() {
    assertEquals("CompositeNode(no child)", composite("k", null).toString());
  }

  @Test
  public void toStringAppendsTheRestOfTheChain() {
    CompositeNode n = composite("k", literal("a"));
    n.setNext(literal("b"));
    assertEquals("CompositeNode(LITERAL(a)) -> LITERAL(b)", n.toString());
  }
}
