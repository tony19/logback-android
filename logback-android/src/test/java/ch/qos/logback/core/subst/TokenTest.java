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
import static org.junit.Assert.assertTrue;

public class TokenTest {

  @Test
  public void tokenEqualsItself() {
    Token token = new Token(Token.Type.LITERAL, "a");
    assertTrue(token.equals(token));
  }

  @Test
  public void tokenIsNotEqualToNullOrToAnotherClass() {
    Token token = new Token(Token.Type.LITERAL, "a");
    assertFalse(token.equals(null));
    assertFalse(token.equals("a"));
    Token subclassInstance = new Token(Token.Type.LITERAL, "a") {
    };
    assertFalse(token.equals(subclassInstance));
  }

  @Test
  public void tokensWithDifferentTypesAreNotEqual() {
    assertNotEquals(new Token(Token.Type.LITERAL, "a"), new Token(Token.Type.START, "a"));
    assertNotEquals(Token.CURLY_LEFT_TOKEN, Token.CURLY_RIGHT_TOKEN);
  }

  @Test
  public void equalsComparesPayload() {
    assertEquals(new Token(Token.Type.START, null), Token.START_TOKEN);
    assertEquals(new Token(Token.Type.LITERAL, "a"), new Token(Token.Type.LITERAL, "a"));
    assertNotEquals(new Token(Token.Type.LITERAL, "a"), new Token(Token.Type.LITERAL, "b"));
    assertNotEquals(new Token(Token.Type.LITERAL, "a"), new Token(Token.Type.LITERAL, null));
    assertNotEquals(new Token(Token.Type.LITERAL, null), new Token(Token.Type.LITERAL, "a"));
  }

  @Test
  public void hashCodeOfTokenWithoutFieldsIsZero() {
    assertEquals(0, new Token(null, null).hashCode());
  }

  @Test
  public void hashCodeCombinesTypeAndPayload() {
    assertEquals(31 * Token.Type.DEFAULT.hashCode(), Token.DEFAULT_SEP_TOKEN.hashCode());
    assertEquals("p".hashCode(), new Token(null, "p").hashCode());
    assertEquals(31 * Token.Type.LITERAL.hashCode() + "p".hashCode(), new Token(Token.Type.LITERAL, "p").hashCode());
  }

  @Test
  public void equalTokensHaveEqualHashCodes() {
    assertEquals(new Token(Token.Type.LITERAL, "a").hashCode(), new Token(Token.Type.LITERAL, "a").hashCode());
  }

  @Test
  public void toStringShowsPayloadWhenPresent() {
    assertEquals("Token{type=LITERAL, payload='abc'}", new Token(Token.Type.LITERAL, "abc").toString());
  }

  @Test
  public void toStringOmitsMissingPayload() {
    assertEquals("Token{type=START}", Token.START_TOKEN.toString());
  }
}
