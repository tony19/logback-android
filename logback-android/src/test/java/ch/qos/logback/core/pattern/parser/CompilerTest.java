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
import ch.qos.logback.core.status.StatusChecker;
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

}
