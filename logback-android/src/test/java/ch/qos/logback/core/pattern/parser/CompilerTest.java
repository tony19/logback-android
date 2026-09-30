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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.Converter123;
import ch.qos.logback.core.pattern.ConverterHello;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.DynamicClassLoadingException;
import ch.qos.logback.core.util.IncompatibleClassException;
import ch.qos.logback.core.util.StatusPrinter;

public class CompilerTest {

  Map<String, String> converterMap = new HashMap<String, String>();
  Context context = new ContextBase();

  @Before
  public void setUp() {
    converterMap.put("OTT", Converter123.class.getName());
    converterMap.put("hello", ConverterHello.class.getName());
    converterMap.putAll(Parser.DEFAULT_COMPOSITE_CONVERTER_MAP);
  }


  String write(final Converter<Object> head, Object event) {
    StringBuilder buf = new StringBuilder();
    Converter<Object> c = head;
    while (c != null) {
      c.write(buf, event);
      c = c.getNext();
    }
    return buf.toString();
  }

  @Test
  public void testLiteral() throws Exception {
    Parser<Object> p = new Parser<Object>("hello");
    Node t = p.parse();
    Converter<Object> head = p.compile(t, converterMap);
    String result = write(head, new Object());
    assertEquals("hello", result);
  }

  @Test
  public void testBasic() throws Exception {
    {
      Parser<Object> p = new Parser<Object>("abc %hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc Hello", result);
    }
    {
      Parser<Object> p = new Parser<Object>("abc %hello %OTT");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc Hello 123", result);
    }
  }

  @Test
  public void testFormat() throws Exception {
    {
      Parser<Object> p = new Parser<Object>("abc %7hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc   Hello", result);
    }

    {
      Parser<Object> p = new Parser<Object>("abc %-7hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc Hello  ", result);
    }

    {
      Parser<Object> p = new Parser<Object>("abc %.3hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc llo", result);
    }

    {
      Parser<Object> p = new Parser<Object>("abc %.-3hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc Hel", result);
    }

    {
      Parser<Object> p = new Parser<Object>("abc %4.5OTT");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc  123", result);
    }
    {
      Parser<Object> p = new Parser<Object>("abc %-4.5OTT");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc 123 ", result);
    }
    {
      Parser<Object> p = new Parser<Object>("abc %3.4hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc ello", result);
    }
    {
      Parser<Object> p = new Parser<Object>("abc %-3.-4hello");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("abc Hell", result);
    }
  }

  @Test
  public void testComposite() throws Exception {
//    {
//      Parser<Object> p = new Parser<Object>("%(ABC)");
//      p.setContext(context);
//      Node t = p.parse();
//      Converter<Object> head = p.compile(t, converterMap);
//      String result = write(head, new Object());
//      assertEquals("ABC", result);
//    }
    {
      Context c = new ContextBase();
      Parser<Object> p = new Parser<Object>("%(ABC %hello)");
      p.setContext(c);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      StatusPrinter.print(c);
      assertEquals("ABC Hello", result);
    }
    {
      Parser<Object> p = new Parser<Object>("%(ABC %hello)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("ABC Hello", result);
    }
  }

  @Test
  public void testCompositeFormatting() throws Exception {
    {
      Parser<Object> p = new Parser<Object>("xyz %4.10(ABC)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz  ABC", result);
    }

    {
      Parser<Object> p = new Parser<Object>("xyz %-4.10(ABC)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz ABC ", result);
    }

    {
      Parser<Object> p = new Parser<Object>("xyz %.2(ABC %hello)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz lo", result);
    }

    {
      Parser<Object> p = new Parser<Object>("xyz %.-2(ABC)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz AB", result);
    }

    {
      Parser<Object> p = new Parser<Object>("xyz %30.30(ABC %20hello)");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz       ABC                Hello", result);
    }
  }

  @Test
  public void testUnknownWord() throws Exception {
    Parser<Object> p = new Parser<Object>("%unknown");
    p.setContext(context);
    Node t = p.parse();
    p.compile(t, converterMap);
    StatusChecker checker = new StatusChecker(context.getStatusManager());
    checker
            .assertContainsMatch("\\[unknown] is not a valid conversion word");
  }

  @Test
  public void testWithNopEscape() throws Exception {
    {
      Parser<Object> p = new Parser<Object>("xyz %hello\\_world");
      p.setContext(context);
      Node t = p.parse();
      Converter<Object> head = p.compile(t, converterMap);
      String result = write(head, new Object());
      assertEquals("xyz Helloworld", result);
    }
  }

  /** Returns the only status whose message is exactly {@code message}. */
  Status statusWithMessage(String message) {
    Status found = null;
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (message.equals(s.getMessage())) {
        assertNull("more than one status with message " + message, found);
        found = s;
      }
    }
    assertNotNull("no status with message " + message, found);
    return found;
  }

  @Test
  public void compositeKeywordWithoutRegisteredConverterBecomesParserError() throws Exception {
    Parser<Object> p = new Parser<Object>("a %foo(x) b");
    p.setContext(context);
    Node t = p.parse();
    Converter<Object> head = p.compile(t, converterMap);

    assertEquals("a %PARSER_ERROR[foo] b", write(head, new Object()));
    Status missing = statusWithMessage(
            "There is no conversion class registered for composite conversion word [foo]");
    assertEquals(Status.ERROR, missing.getLevel());
    Status failed = statusWithMessage("Failed to create converter for [%foo] keyword");
    assertEquals(Status.ERROR, failed.getLevel());
  }

  @Test
  public void compositeKeywordMappedToNonCompositeConverterBecomesParserError() throws Exception {
    Parser<Object> p = new Parser<Object>("%hello(x)");
    p.setContext(context);
    Node t = p.parse();
    Converter<Object> head = p.compile(t, converterMap);

    assertEquals("%PARSER_ERROR[hello]", write(head, new Object()));
    Status instantiation = statusWithMessage("Failed to instantiate converter class ["
            + ConverterHello.class.getName() + "] as a composite converter for keyword [hello]");
    assertEquals(Status.ERROR, instantiation.getLevel());
    assertTrue(instantiation.getThrowable() instanceof IncompatibleClassException);
    Status failed = statusWithMessage("Failed to create converter for [%hello] keyword");
    assertEquals(Status.ERROR, failed.getLevel());
  }

  @Test
  public void keywordMappedToMissingClassBecomesParserError() throws Exception {
    converterMap.put("missing", "no.such.Converter");
    Parser<Object> p = new Parser<Object>("a %missing b");
    p.setContext(context);
    Node t = p.parse();
    Converter<Object> head = p.compile(t, converterMap);

    assertEquals("a %PARSER_ERROR[missing] b", write(head, new Object()));
    Status instantiation = statusWithMessage(
            "Failed to instantiate converter class [no.such.Converter] for keyword [missing]");
    assertEquals(Status.ERROR, instantiation.getLevel());
    assertTrue(instantiation.getThrowable() instanceof DynamicClassLoadingException);
    Status invalid = statusWithMessage("[missing] is not a valid conversion word");
    assertEquals(Status.ERROR, invalid.getLevel());
  }

  @Test
  public void nodesOfUnknownTypeAreSkipped() {
    Node top = new Node(99, "ignored");
    top.setNext(new Node(Node.LITERAL, "a"));
    Compiler<Object> compiler = new Compiler<Object>(top, converterMap);
    compiler.setContext(context);

    Converter<Object> head = compiler.compile();

    assertEquals("a", write(head, new Object()));
    assertNull(head.getNext());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

}
