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
import static org.junit.Assert.assertThrows;

public class NodeTest {

  private static Node literal(String s) {
    return new Node(Node.Type.LITERAL, s);
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
}
