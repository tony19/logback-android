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
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TokenTest {

  @Test
  public void constructorsSetTypeValueAndOptions() {
    List<String> options = Arrays.asList("a", "b");

    Token typeOnly = new Token(Token.PERCENT);
    assertEquals(Token.PERCENT, typeOnly.getType());
    assertNull(typeOnly.getValue());
    assertNull(typeOnly.getOptionsList());

    Token withValue = new Token(Token.LITERAL, "v");
    assertEquals(Token.LITERAL, withValue.getType());
    assertEquals("v", withValue.getValue());
    assertNull(withValue.getOptionsList());

    Token withOptions = new Token(Token.OPTION, options);
    assertEquals(Token.OPTION, withOptions.getType());
    assertNull(withOptions.getValue());
    assertSame(options, withOptions.getOptionsList());

    Token full = new Token(Token.SIMPLE_KEYWORD, "k", options);
    assertEquals(Token.SIMPLE_KEYWORD, full.getType());
    assertEquals("k", full.getValue());
    assertSame(options, full.getOptionsList());
  }

  @Test
  public void toStringNamesEachTokenType() {
    assertEquals("Token(%)", Token.PERCENT_TOKEN.toString());
    assertEquals("Token(FormatModifier, \"-5\")", new Token(Token.FORMAT_MODIFIER, "-5").toString());
    assertEquals("Token(LITERAL, \"abc\")", new Token(Token.LITERAL, "abc").toString());
    assertEquals("Token(OPTION)", new Token(Token.OPTION, Collections.singletonList("o")).toString());
    assertEquals("Token(SIMPLE_KEYWORD, \"x\")", new Token(Token.SIMPLE_KEYWORD, "x").toString());
    assertEquals("Token(COMPOSITE_KEYWORD, \"BARE\")", Token.BARE_COMPOSITE_KEYWORD_TOKEN.toString());
    assertEquals("Token(RIGHT_PARENTHESIS)", Token.RIGHT_PARENTHESIS_TOKEN.toString());
    assertEquals("Token(UNKNOWN, \"EOF\")", Token.EOF_TOKEN.toString());
    assertEquals("Token(UNKNOWN)", new Token(-1).toString());
  }

  @Test
  public void hashCodeCombinesTypeAndValue() {
    assertEquals(29 * Token.LITERAL + "abc".hashCode(), new Token(Token.LITERAL, "abc").hashCode());
    assertEquals(29 * Token.PERCENT, new Token(Token.PERCENT).hashCode());
  }

  @Test
  public void equalTokensHaveEqualHashCodes() {
    Token a = new Token(Token.LITERAL, "abc");
    Token b = new Token(Token.LITERAL, "abc");
    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void tokenEqualsItself() {
    Token t = new Token(Token.LITERAL, "abc");
    assertTrue(t.equals(t));
  }

  @Test
  public void tokenDoesNotEqualNonToken() {
    assertFalse(new Token(Token.LITERAL, "abc").equals("abc"));
    assertFalse(new Token(Token.LITERAL, "abc").equals(null));
  }

  @Test
  public void tokensWithDifferentTypesAreNotEqual() {
    assertFalse(new Token(Token.LITERAL, "x").equals(new Token(Token.SIMPLE_KEYWORD, "x")));
  }

  @Test
  public void tokensWithDifferentValuesAreNotEqual() {
    assertFalse(new Token(Token.LITERAL, "x").equals(new Token(Token.LITERAL, "y")));
    assertFalse(new Token(Token.LITERAL, "x").equals(new Token(Token.LITERAL)));
    assertFalse(new Token(Token.LITERAL).equals(new Token(Token.LITERAL, "x")));
  }

  @Test
  public void valuelessTokensOfSameTypeAreEqual() {
    assertTrue(new Token(Token.RIGHT_PARENTHESIS).equals(Token.RIGHT_PARENTHESIS_TOKEN));
  }

  @Test
  public void equalsIgnoresOptionsList() {
    Token a = new Token(Token.OPTION, Collections.singletonList("a"));
    Token b = new Token(Token.OPTION, Collections.singletonList("b"));
    assertTrue(a.equals(b));
  }
}
