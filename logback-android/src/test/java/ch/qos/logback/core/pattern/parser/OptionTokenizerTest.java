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

import org.junit.Test;

public class OptionTokenizerTest {

  @Test
   public void testEmpty() {

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