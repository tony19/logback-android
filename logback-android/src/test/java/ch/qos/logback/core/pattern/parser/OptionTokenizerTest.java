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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.core.spi.ScanException;

public class OptionTokenizerTest {

  static final String UNEXPECTED_END = "Unexpected end of pattern string in OptionTokenizer";

  @Test
   public void testEmpty() {

  }

  @Test
  public void escapeConsumingLastCharOfQuotedOptionIsReportedAsScanException() {
    // the backslash escapes the closing quote, which is the last char of the pattern
    ScanException e = assertThrows(ScanException.class,
        () -> new TokenStream("%x{'a\\'").tokenize());
    assertEquals(UNEXPECTED_END, e.getMessage());
  }

  @Test
  public void unterminatedQuotedOptionEndingWithCurlyBraceIsRejected() {
    // the '}' is inside the still-open quote, so it does not close the option list
    ScanException e = assertThrows(ScanException.class,
        () -> new TokenStream("%x{'abc}").tokenize());
    assertEquals(UNEXPECTED_END, e.getMessage());
  }

  @Test
  public void optionWithoutClosingCurlyBraceIsRejected() {
    ScanException e = assertThrows(ScanException.class,
        () -> new TokenStream("%x{abc").tokenize());
    assertEquals(UNEXPECTED_END, e.getMessage());
  }

  @Test
  public void quotedOptionWithoutClosingCurlyBraceIsRejected() {
    ScanException e = assertThrows(ScanException.class,
        () -> new TokenStream("%x{'abc'").tokenize());
    assertEquals(UNEXPECTED_END, e.getMessage());
  }

  @Test
  public void escapedQuoteDoesNotCloseQuotedOptionAndIsKeptAsIs() throws ScanException {
    List<Token> tokens = new TokenStream("%x{'a\\'b'}").tokenize();

    assertEquals(3, tokens.size());
    Token option = tokens.get(2);
    assertEquals(Token.OPTION, option.getType());
    assertEquals(Collections.singletonList("a\\'b"), option.getOptionsList());
  }

  @Test
  public void escapeAtEndOfPatternIsNoOp() {
    TokenStream ts = new TokenStream("x");
    ts.pointer = 1;
    OptionTokenizer ot = new OptionTokenizer(ts);
    StringBuffer buf = new StringBuffer("a");

    ot.escape("'", buf);

    assertEquals("a", buf.toString());
    assertEquals(1, ts.pointer);
  }

  @Test
  public void unknownStateConsumesRestOfPatternWithoutEmittingOption() {
    TokenStream ts = new TokenStream("ab}");
    ts.pointer = 1;
    OptionTokenizer ot = new OptionTokenizer(ts);
    ot.state = 42;
    List<Token> tokens = new ArrayList<Token>();

    ScanException e = assertThrows(ScanException.class, () -> ot.tokenize('a', tokens));

    assertEquals(UNEXPECTED_END, e.getMessage());
    assertTrue(tokens.isEmpty());
    assertEquals(3, ts.pointer);
  }

//
//  @Test
//  public void testEmpty() throws ScanException {
//    {
//      List ol = new OptionTokenizer("").tokenize();
//      List witness = new ArrayList();
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer(" ").tokenize();
//      List witness = new ArrayList();
//      assertEquals(witness, ol);
//    }
//  }
//
//  @Test
//  public void testSimple() throws ScanException {
//    {
//      List ol = new OptionTokenizer("abc").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add("abc");
//      assertEquals(witness, ol);
//    }
//  }
//
//  @Test
//  public void testSingleQuote() throws ScanException {
//    {
//      List ol = new OptionTokenizer("' '").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" ");
//      assertEquals(witness, ol);
//    }
//
//    {
//     List ol = new OptionTokenizer("' x\t'").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\t");
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer("' x\\t'").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\\t");
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer("' x\\''").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\\'");
//      assertEquals(witness, ol);
//    }
//  }
//
//
//
//  @Test
//  public void testDoubleQuote() throws ScanException {
//    {
//      List ol = new OptionTokenizer("\" \"").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" ");
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer("\" x\t\"").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\t");
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer("\" x\\t\"").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\\t");
//      assertEquals(witness, ol);
//    }
//
//    {
//      List ol = new OptionTokenizer("\" x\\\"\"").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add(" x\\\"");
//      assertEquals(witness, ol);
//    }
//  }
//
//  @Test
//  public void testMultiple() throws ScanException {
//    {
//      List ol = new OptionTokenizer("a, b").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add("a");
//      witness.add("b");
//      assertEquals(witness, ol);
//    }
//    {
//      List ol = new OptionTokenizer("'a', b").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add("a");
//      witness.add("b");
//      assertEquals(witness, ol);
//    }
//    {
//      List ol = new OptionTokenizer("'', b").tokenize();
//      List<String> witness = new ArrayList<String>();
//      witness.add("");
//      witness.add("b");
//      assertEquals(witness, ol);
//    }
//  }
//
}