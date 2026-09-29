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
package ch.qos.logback.core.subst;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class NodeTest {

  private static Node literal(String s) {
    return new Node(Node.Type.LITERAL, s);
  }

  /** A payload that is never equal to anything, not even to itself. */
  private static final Object NEVER_EQUAL = new Object() {
    @Override
    public boolean equals(Object o) {
      return false;
    }

    @Override
    public int hashCode() {
      return 7;
    }
  };

  @Test
  public void threeArgConstructorSetsTypePayloadAndDefaultPart() {
    Node payload = literal("k");
    Node defaultPart = literal("d");

    Node node = new Node(Node.Type.VARIABLE, payload, defaultPart);

    assertSame(Node.Type.VARIABLE, node.type);
    assertSame(payload, node.payload);
    assertSame(defaultPart, node.defaultPart);
    assertNull(node.next);
  }

  @Test
  public void twoArgConstructorLeavesDefaultPartUnset() {
    Node node = new Node(Node.Type.VARIABLE, literal("k"));
    assertNull(node.defaultPart);
  }

  @Test
  public void setNextLinksNodes() {
    Node a = literal("a");
    Node b = literal("b");

    a.setNext(b);

    assertSame(b, a.next);
    a.setNext(null);
    assertNull(a.next);
  }

  @Test
  public void literalToStringShowsPayload() {
    assertEquals("Node{type=LITERAL, payload='abc'}", literal("abc").toString());
  }

  @Test
  public void variableToStringWithoutDefaultPartShowsPayloadChain() {
    Node node = new Node(Node.Type.VARIABLE, literal("k"));
    assertEquals("Node{type=VARIABLE, payload='Node{type=LITERAL, payload='k'} --> null '}", node.toString());
  }

  @Test
  public void variableToStringWithDefaultPartShowsBothChains() {
    Node payload = literal("a");
    payload.next = literal("b");
    Node node = new Node(Node.Type.VARIABLE, payload, literal("x"));

    assertEquals("Node{type=VARIABLE, payload='Node{type=LITERAL, payload='a'} --> Node{type=LITERAL, payload='b'} --> null '"
        + ", defaultPart=Node{type=LITERAL, payload='x'} --> null }", node.toString());
  }

  @Test
  public void variableToStringWithoutPayloadShowsEmptyChain() {
    Node node = new Node(Node.Type.VARIABLE, null);
    assertEquals("Node{type=VARIABLE, payload='null '}", node.toString());
  }

  @Test
  public void toStringOfNodeWithoutTypeFails() {
    Node node = new Node(null, "a");
    assertThrows(NullPointerException.class, node::toString);
  }

  @Test
  public void recursiveAppendsEveryNodeOfTheChain() {
    Node chain = literal("a");
    chain.next = literal("b");
    StringBuilder sb = new StringBuilder("> ");

    chain.recursive(chain, sb);

    assertEquals("> Node{type=LITERAL, payload='a'} --> Node{type=LITERAL, payload='b'} --> null ", sb.toString());
  }

  @Test
  public void recursiveOnNullChainAppendsOnlyTerminator() {
    StringBuilder sb = new StringBuilder();
    literal("a").recursive(null, sb);
    assertEquals("null ", sb.toString());
  }

  @Test
  public void nodeEqualsItselfEvenWhenPayloadDoesNot() {
    Node node = new Node(Node.Type.LITERAL, NEVER_EQUAL);
    assertTrue(node.equals(node));
    assertFalse(node.equals(new Node(Node.Type.LITERAL, NEVER_EQUAL)));
  }

  @Test
  public void nodeIsNotEqualToNullOrToAnotherClass() {
    Node node = literal("a");
    assertFalse(node.equals(null));
    assertFalse(node.equals("a"));
    Node subclassInstance = new Node(Node.Type.LITERAL, "a") {
    };
    assertFalse(node.equals(subclassInstance));
  }

  @Test
  public void nodesWithDifferentTypesAreNotEqual() {
    assertNotEquals(new Node(Node.Type.LITERAL, "a"), new Node(Node.Type.VARIABLE, "a"));
  }

  @Test
  public void equalsComparesPayload() {
    assertEquals(new Node(Node.Type.LITERAL, null), new Node(Node.Type.LITERAL, null));
    assertEquals(literal("a"), literal("a"));
    assertNotEquals(literal("a"), literal("b"));
    assertNotEquals(literal("a"), new Node(Node.Type.LITERAL, null));
    assertNotEquals(new Node(Node.Type.LITERAL, null), literal("a"));
  }

  @Test
  public void equalsComparesDefaultPart() {
    Node withoutDefault = new Node(Node.Type.VARIABLE, literal("k"));
    Node withDefaultX = new Node(Node.Type.VARIABLE, literal("k"), literal("x"));

    assertEquals(withDefaultX, new Node(Node.Type.VARIABLE, literal("k"), literal("x")));
    assertNotEquals(withDefaultX, new Node(Node.Type.VARIABLE, literal("k"), literal("y")));
    assertNotEquals(withDefaultX, withoutDefault);
    assertNotEquals(withoutDefault, withDefaultX);
  }

  @Test
  public void equalsComparesNext() {
    Node a1 = literal("a");
    a1.next = literal("b");
    Node a2 = literal("a");
    a2.next = literal("b");
    Node a3 = literal("a");
    a3.next = literal("c");
    Node aAlone = literal("a");

    assertEquals(a1, a2);
    assertNotEquals(a1, a3);
    assertNotEquals(a1, aAlone);
    assertNotEquals(aAlone, a1);
  }

  @Test
  public void hashCodeOfNodeWithoutFieldsIsZero() {
    assertEquals(0, new Node(null, null).hashCode());
  }

  @Test
  public void hashCodeCombinesEveryField() {
    int typeHash = Node.Type.LITERAL.hashCode();
    assertEquals(31 * 31 * 31 * typeHash, new Node(Node.Type.LITERAL, null).hashCode());
    assertEquals(31 * 31 * "p".hashCode(), new Node(null, "p").hashCode());
    assertEquals(31 * "d".hashCode(), new Node(null, null, "d").hashCode());

    Node withNext = new Node(null, null);
    withNext.next = new Node(null, "n");
    assertEquals(31 * 31 * "n".hashCode(), withNext.hashCode());
  }

  @Test
  public void equalNodesHaveEqualHashCodes() {
    Node n1 = new Node(Node.Type.VARIABLE, literal("k"), literal("x"));
    n1.next = literal("tail");
    Node n2 = new Node(Node.Type.VARIABLE, literal("k"), literal("x"));
    n2.next = literal("tail");

    assertEquals(n1, n2);
    assertEquals(n1.hashCode(), n2.hashCode());
  }
}
