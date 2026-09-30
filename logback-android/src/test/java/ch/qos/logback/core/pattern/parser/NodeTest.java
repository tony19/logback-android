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

public class NodeTest {

  static Node literal(String value) {
    return new Node(Node.LITERAL, value);
  }

  static Node chain(Node first, Node next) {
    first.setNext(next);
    return first;
  }

  @Test
  public void typeOnlyConstructorLeavesValueNull() {
    Node n = new Node(Node.SIMPLE_KEYWORD);
    assertEquals(Node.SIMPLE_KEYWORD, n.getType());
    assertNull(n.getValue());
    assertNull(n.getNext());
  }

  @Test
  public void setNextLinksNodes() {
    Node first = literal("a");
    Node second = literal("b");
    first.setNext(second);
    assertSame(second, first.getNext());
  }

  @Test
  public void nodeEqualsItself() {
    Node n = literal("a");
    assertTrue(n.equals(n));
  }

  @Test
  public void nodeDoesNotEqualNonNode() {
    assertFalse(literal("a").equals("a"));
    assertFalse(literal("a").equals(null));
  }

  @Test
  public void nodesWithDifferentTypesAreNotEqual() {
    assertFalse(literal("a").equals(new Node(Node.SIMPLE_KEYWORD, "a")));
  }

  @Test
  public void nodesWithDifferentValuesAreNotEqual() {
    assertFalse(literal("a").equals(literal("b")));
    assertFalse(literal("a").equals(new Node(Node.LITERAL)));
    assertFalse(new Node(Node.LITERAL).equals(literal("a")));
  }

  @Test
  public void valuelessNodesOfSameTypeAreEqual() {
    assertTrue(new Node(Node.LITERAL).equals(new Node(Node.LITERAL)));
  }

  @Test
  public void equalsComparesTheRestOfTheChain() {
    assertTrue(chain(literal("a"), literal("b")).equals(chain(literal("a"), literal("b"))));
    assertFalse(chain(literal("a"), literal("b")).equals(chain(literal("a"), literal("c"))));
    assertFalse(chain(literal("a"), literal("b")).equals(literal("a")));
    assertFalse(literal("a").equals(chain(literal("a"), literal("b"))));
  }

  @Test
  public void hashCodeCombinesTypeAndValue() {
    assertEquals(31 * Node.SIMPLE_KEYWORD + "x".hashCode(), new Node(Node.SIMPLE_KEYWORD, "x").hashCode());
    assertEquals(31 * Node.SIMPLE_KEYWORD, new Node(Node.SIMPLE_KEYWORD).hashCode());
  }

  @Test
  public void hashCodeIgnoresNext() {
    assertEquals(literal("a").hashCode(), chain(literal("a"), literal("b")).hashCode());
  }

  @Test
  public void literalToStringShowsValue() {
    assertEquals("LITERAL(a)", literal("a").toString());
  }

  @Test
  public void toStringAppendsTheRestOfTheChain() {
    assertEquals("LITERAL(a) -> LITERAL(b) -> LITERAL(c)",
        chain(literal("a"), chain(literal("b"), literal("c"))).toString());
  }

  @Test
  public void nonLiteralToStringFallsBackToObjectToString() {
    Node n = new Node(Node.SIMPLE_KEYWORD, "x");
    String identity = Node.class.getName() + "@" + Integer.toHexString(n.hashCode());
    assertEquals(identity, n.toString());

    n.setNext(literal("y"));
    assertEquals(identity + " -> LITERAL(y)", n.toString());
  }

  @Test
  public void printNextIsEmptyAtEndOfChain() {
    assertEquals("", literal("a").printNext());
    assertEquals(" -> LITERAL(b)", chain(literal("a"), literal("b")).printNext());
  }
}
