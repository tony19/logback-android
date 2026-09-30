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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.pattern.FormatInfo;

public class SimpleKeywordNodeTest {

  static SimpleKeywordNode keyword(String value, List<String> options) {
    SimpleKeywordNode n = new SimpleKeywordNode(value);
    n.setOptions(options);
    return n;
  }

  @Test
  public void keywordNodeHasSimpleKeywordType() {
    SimpleKeywordNode n = keyword("x", Arrays.asList("a"));
    assertEquals(Node.SIMPLE_KEYWORD, n.getType());
    assertEquals("x", n.getValue());
    assertEquals(Arrays.asList("a"), n.getOptions());
  }

  @Test
  public void nodesWithSameOptionsAreEqual() {
    assertTrue(keyword("x", Arrays.asList("a")).equals(keyword("x", Arrays.asList("a"))));
    assertTrue(keyword("x", null).equals(keyword("x", null)));
  }

  @Test
  public void nodesDifferingAsFormattingNodesAreNotEqual() {
    SimpleKeywordNode formatted = keyword("x", null);
    formatted.setFormatInfo(new FormatInfo(1, 2));
    assertFalse(formatted.equals(keyword("x", null)));
  }

  @Test
  public void keywordNodeDoesNotEqualPlainFormattingNode() {
    assertFalse(keyword("x", null).equals(new FormattingNode(Node.SIMPLE_KEYWORD, "x")));
  }

  @Test
  public void nodesWithDifferentOptionsAreNotEqual() {
    assertFalse(keyword("x", Arrays.asList("a")).equals(keyword("x", Arrays.asList("b"))));
    assertFalse(keyword("x", Arrays.asList("a")).equals(keyword("x", null)));
    assertFalse(keyword("x", null).equals(keyword("x", Arrays.asList("a"))));
  }

  @Test
  public void hashCodeIsFormattingNodeHashCode() {
    SimpleKeywordNode n = keyword("x", Arrays.asList("a"));
    n.setFormatInfo(new FormatInfo(1, 2));
    FormattingNode f = new FormattingNode(Node.SIMPLE_KEYWORD, "x");
    f.setFormatInfo(new FormatInfo(1, 2));
    assertEquals(f.hashCode(), n.hashCode());
  }

  @Test
  public void toStringWithoutOptions() {
    assertEquals("KeyWord(x,null)", keyword("x", null).toString());

    SimpleKeywordNode formatted = keyword("x", null);
    formatted.setFormatInfo(new FormatInfo(1, 2));
    assertEquals("KeyWord(x,FormatInfo(1, 2, true, true))", formatted.toString());
  }

  @Test
  public void toStringWithOptions() {
    assertEquals("KeyWord(x, null,[a, b])", keyword("x", Arrays.asList("a", "b")).toString());
  }

  @Test
  public void toStringAppendsTheRestOfTheChain() {
    SimpleKeywordNode n = keyword("x", null);
    n.setNext(new Node(Node.LITERAL, "y"));
    assertEquals("KeyWord(x,null) -> LITERAL(y)", n.toString());
  }
}
