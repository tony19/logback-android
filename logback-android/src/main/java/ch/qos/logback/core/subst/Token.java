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

public class Token {

  static public final Token START_TOKEN = new Token(Type.START, null);
  static public final Token CURLY_LEFT_TOKEN = new Token(Type.CURLY_LEFT, null);
  static public final Token CURLY_RIGHT_TOKEN = new Token(Type.CURLY_RIGHT, null);
  static public final Token DEFAULT_SEP_TOKEN = new Token(Type.DEFAULT, null);

  public enum Type {LITERAL, START, CURLY_LEFT, CURLY_RIGHT, DEFAULT}

  Type type;
  String payload;

  public Token(Type type, String payload) {
    this.type = type;
    this.payload = payload;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Token token = (Token) o;

    if (type != token.type) return false;
    if (payload != null ? !payload.equals(token.payload) : token.payload != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (payload != null ? payload.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    String result = "Token{" +
            "type=" + type;
    if (payload != null)
      result += ", payload='" + payload + '\'';

    result += '}';
    return result;
  }
}
