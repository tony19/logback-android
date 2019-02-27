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

import static junit.framework.Assert.*;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.PatternLayoutBase;


public class SamplePatternLayoutTest extends AbstractPatternLayoutBaseTest<Object> {

  Context context = new ContextBase();

  public PatternLayoutBase<Object> getPatternLayoutBase() {
    return new SamplePatternLayout<Object>();
  }

  public Object getEventObject() {
    return new Object();
  }
  
  @Test
  public void testOK() {
    PatternLayoutBase<Object> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern("x%OTT");
    plb.start();
    String s = plb.doLayout(new Object());
    //System.out.println(s);

    //StatusManager sm = context.getStatusManager();
    //StatusPrinter.print(sm);
    assertEquals("x123", s);
  }

  @Test
  public void testEscapeClosingParentheses() {
    PatternLayoutBase<Object> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern("x(%OTT\\)y");
    plb.start();
    String s = plb.doLayout(new Object());
    assertEquals("x(123)y", s);
  }
  
  @Test
  public void testEscapeBothParentheses() {
    PatternLayoutBase<Object> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern("x\\(%OTT\\)y");
    plb.start();
    String s = plb.doLayout(new Object());
    assertEquals("x(123)y", s);
  }

  @Test
  public void testPercentAsLiteral() {
    PatternLayoutBase<Object> plb = getPatternLayoutBase();
    Context context = new ContextBase();
    plb.setContext(context);
    plb.setPattern("hello \\% world");
    plb.start();
    String s = plb.doLayout(new Object());
    assertEquals("hello % world", s);
  }

  
  @Override
  public Context getContext() {
    return  context;
  }
}
