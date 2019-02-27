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


import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ScanException;

import java.util.List;

// E = TE|T
//   = T(E|~)
// E = TEopt where Eopt = E|~
// T = LITERAL | { C } |'${' V '}'
// C = E|E :- E
//   = E(':-'E|~)
// V = E|E :- E
//   = E(':-'E|~)

/**
 * Parse a token list returning a node chain.
 *
 * @author Ceki Gulcu
 */
public class Parser {

  final List<Token> tokenList;
  int pointer = 0;

  public Parser(List<Token> tokenList) {
    this.tokenList = tokenList;
  }

  public Node parse() throws ScanException {
    if (tokenList == null || tokenList.isEmpty())
      return null;
    return E();
  }

  private Node E() throws ScanException {
    Node t = T();
    if (t == null) {
      return null;
    }
    Node eOpt = Eopt();
    if (eOpt != null) {
      t.append(eOpt);
    }
    return t;
  }

  // Eopt = E|~
  private Node Eopt() throws ScanException {
    Token next = peekAtCurentToken();
    if (next == null) {
      return null;
    } else {
      return E();
    }
  }

  // T = LITERAL | '${' V '}'
  private Node T() throws ScanException {
    Token t = peekAtCurentToken();

    switch (t.type) {
      case LITERAL:
        advanceTokenPointer();
        return makeNewLiteralNode(t.payload);
      case CURLY_LEFT:
        advanceTokenPointer();
        Node innerNode = C();
        Token right = peekAtCurentToken();
        expectCurlyRight(right);
        advanceTokenPointer();
        Node curlyLeft = makeNewLiteralNode(CoreConstants.LEFT_ACCOLADE);
        curlyLeft.append(innerNode);
        curlyLeft.append(makeNewLiteralNode(CoreConstants.RIGHT_ACCOLADE));
        return curlyLeft;
      case START:
        advanceTokenPointer();
        Node v = V();
        Token w = peekAtCurentToken();
        expectCurlyRight(w);
        advanceTokenPointer();
        return v;
      default:
        return null;
    }
  }

  private Node makeNewLiteralNode(String s) {
    return new Node(Node.Type.LITERAL, s);
  }

  // V = E(':='E|~)
  private Node V() throws ScanException {
    Node e = E();
    Node variable = new Node(Node.Type.VARIABLE, e);
    Token t = peekAtCurentToken();
    if (isDefaultToken(t)) {
      advanceTokenPointer();
      Node def = E();
      variable.defaultPart = def;
    }
    return variable;
  }

  // C = E(':='E|~)
  private Node C() throws ScanException {
    Node e0 = E();
    Token t = peekAtCurentToken();
    if (isDefaultToken(t)) {
      advanceTokenPointer();
      Node literal = makeNewLiteralNode(CoreConstants.DEFAULT_VALUE_SEPARATOR);
      e0.append(literal);
      Node e1 = E();
      e0.append(e1);
    }
    return e0;
  }

  private boolean isDefaultToken(Token t) {
    return t != null && t.type == Token.Type.DEFAULT;
  }

  void advanceTokenPointer() {
    pointer++;
  }

  void expectNotNull(Token t, String expected) {
    if (t == null) {
      throw new IllegalArgumentException("All tokens consumed but was expecting \""
              + expected + "\"");
    }
  }

  void expectCurlyRight(Token t) throws ScanException {
    expectNotNull(t, "}");
    if (t.type != Token.Type.CURLY_RIGHT) {
      throw new ScanException("Expecting }");
    }
  }

  Token peekAtCurentToken() {
    if (pointer < tokenList.size()) {
      return tokenList.get(pointer);
    }
    return null;
  }

}
