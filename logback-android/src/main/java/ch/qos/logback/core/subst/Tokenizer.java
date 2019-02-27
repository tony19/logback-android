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

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

  enum TokenizerState {LITERAL_STATE, START_STATE, DEFAULT_VAL_STATE}

  final String pattern;
  final int patternLength;

  public Tokenizer(String pattern) {
    this.pattern = pattern;
    patternLength = pattern.length();
  }

  TokenizerState state = TokenizerState.LITERAL_STATE;
  int pointer = 0;

  List<Token> tokenize() throws ScanException {
    List<Token> tokenList = new ArrayList<Token>();
    StringBuilder buf = new StringBuilder();

    while (pointer < patternLength) {
      char c = pattern.charAt(pointer);
      pointer++;

      switch (state) {
        case LITERAL_STATE:
          handleLiteralState(c, tokenList, buf);
          break;
        case START_STATE:
          handleStartState(c, tokenList, buf);
          break;
        case DEFAULT_VAL_STATE:
          handleDefaultValueState(c, tokenList, buf);
        default:
      }
    }
    // EOS
    switch (state) {
      case LITERAL_STATE:
        addLiteralToken(tokenList, buf);
        break;
      case DEFAULT_VAL_STATE:
        // trailing colon. see also LOGBACK-1140
        buf.append(CoreConstants.COLON_CHAR);
        addLiteralToken(tokenList, buf);
        break;
      case START_STATE:
        // trailing $. see also LOGBACK-1149
        buf.append(CoreConstants.DOLLAR);
        addLiteralToken(tokenList, buf);
        break;
    }
    return tokenList;
  }

  private void handleDefaultValueState(char c, List<Token> tokenList, StringBuilder stringBuilder) {
    switch(c) {
      case  CoreConstants.DASH_CHAR:
        tokenList.add(Token.DEFAULT_SEP_TOKEN);
        state = TokenizerState.LITERAL_STATE;
        break;
      case CoreConstants.DOLLAR:
        stringBuilder.append(CoreConstants.COLON_CHAR);
        addLiteralToken(tokenList, stringBuilder);
        stringBuilder.setLength(0);
        state = TokenizerState.START_STATE;
        break;
      case CoreConstants.CURLY_LEFT:
        stringBuilder.append(CoreConstants.COLON_CHAR);
        addLiteralToken(tokenList, stringBuilder);
        stringBuilder.setLength(0);
        tokenList.add(Token.CURLY_LEFT_TOKEN);
        state = TokenizerState.LITERAL_STATE;
        break;
      default:
        stringBuilder.append(CoreConstants.COLON_CHAR).append(c);
        state = TokenizerState.LITERAL_STATE;
        break;
    }
  }

  private void handleStartState(char c, List<Token> tokenList, StringBuilder stringBuilder) {
    if (c == CoreConstants.CURLY_LEFT) {
      tokenList.add(Token.START_TOKEN);
    } else {
      stringBuilder.append(CoreConstants.DOLLAR).append(c);
    }
    state = TokenizerState.LITERAL_STATE;
  }

  private void handleLiteralState(char c, List<Token> tokenList, StringBuilder stringBuilder) {
    switch (c) {
      case CoreConstants.DOLLAR:
        addLiteralToken(tokenList, stringBuilder);
        stringBuilder.setLength(0);
        state = TokenizerState.START_STATE;
        break;
      case CoreConstants.COLON_CHAR:
        addLiteralToken(tokenList, stringBuilder);
        stringBuilder.setLength(0);
        state = TokenizerState.DEFAULT_VAL_STATE;
        break;
      case CoreConstants.CURLY_LEFT:
        addLiteralToken(tokenList, stringBuilder);
        tokenList.add(Token.CURLY_LEFT_TOKEN);
        stringBuilder.setLength(0);
        break;
      case CoreConstants.CURLY_RIGHT:
        addLiteralToken(tokenList, stringBuilder);
        tokenList.add(Token.CURLY_RIGHT_TOKEN);
        stringBuilder.setLength(0);
        break;
      default:
        stringBuilder.append(c);
    }
  }

  private void addLiteralToken(List<Token> tokenList, StringBuilder stringBuilder) {
    if (stringBuilder.length() == 0)
      return;
    tokenList.add(new Token(Token.Type.LITERAL, stringBuilder.toString()));
  }


}


