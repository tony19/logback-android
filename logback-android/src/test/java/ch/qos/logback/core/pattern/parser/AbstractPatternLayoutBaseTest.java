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
import static junit.framework.Assert.assertFalse;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.ExceptionalConverter;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.StatusPrinter;


abstract public class AbstractPatternLayoutBaseTest<E> {
  
  abstract public PatternLayoutBase<E> getPatternLayoutBase();
  abstract public E getEventObject();
  abstract public Context getContext();
  
  @Test
  public void testUnStarted() {
    PatternLayoutBase<E> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    String s = plb.doLayout(getEventObject());
    assertEquals("", s);
    StatusManager sm = context.getStatusManager();
    StatusPrinter.print(sm);
  }

  /**
   * This test checks that the pattern layout implementation starts its
   * converters. ExceptionalConverter throws an exception if it's convert
   * method is called before being started.
   */
  @Test
  public void testConverterStart() {
    PatternLayoutBase<E> plb = getPatternLayoutBase();
    plb.setContext(getContext());
    plb.getInstanceConverterMap().put("EX", ExceptionalConverter.class.getName());
    plb.setPattern("%EX");
    plb.start();
    String result = plb.doLayout(getEventObject());
    assertFalse(result.contains("%PARSER_ERROR_EX"));
    //System.out.println("========="+result);
  }

  @Test
  public void testStarted() {
    PatternLayoutBase<E> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    String s = plb.doLayout(getEventObject());
    assertEquals("", s);
    StatusManager sm = context.getStatusManager();
    StatusPrinter.print(sm);
  }

  @Test
  public void testNullPattern() {
    //System.out.println("testNullPattern");
    PatternLayoutBase<E> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern(null);
    plb.start();
    String s = plb.doLayout(getEventObject());
    assertEquals("", s);
    StatusChecker checker = new StatusChecker(context.getStatusManager());
    //StatusPrinter.print(context);
    checker.assertContainsMatch("Empty or null pattern.");
  }

  @Test
  public void testEmptyPattern() {
    //System.out.println("testNullPattern");
    PatternLayoutBase<E> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern("");
    plb.start();
    String s = plb.doLayout(getEventObject());
    assertEquals("", s);
    StatusChecker checker = new StatusChecker(context.getStatusManager());
    //StatusPrinter.print(context);
    checker.assertContainsMatch("Empty or null pattern.");
  }

}
