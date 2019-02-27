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
package ch.qos.logback.classic.spi;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import static ch.qos.logback.classic.util.TestHelper.addSuppressed;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import ch.qos.logback.classic.util.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThrowableProxyTest {

  StringWriter sw = new StringWriter();
  PrintWriter pw = new PrintWriter(sw);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  public void verify(Throwable t) {
    t.printStackTrace(pw);

    IThrowableProxy tp = new ThrowableProxy(t);

    String result = ThrowableProxyUtil.asString(tp);
    result = result.replace("common frames omitted", "more");

    String expected = sw.toString();

    System.out.println("========expected");
    System.out.println(expected);

    System.out.println("========result");
    System.out.println(result);

    assertEquals(expected, result);
  }

  public void verifyContains(Throwable t, String expected) {
    IThrowableProxy tp = new ThrowableProxy(t);
    String result = ThrowableProxyUtil.asString(tp);

    assertTrue("Did not find '" + expected + "' in \n" + result, result.contains(expected));
  }

  @Test
  public void smoke() {
    Exception e = new Exception("smoke");
    verify(e);
  }

  @Test
  public void nested() {
    Exception w = null;
    try {
      someMethod();
    } catch (Exception e) {
      w = new Exception("wrapping", e);
    }
    verify(w);
  }

  @Test
  public void suppressed() throws InvocationTargetException, IllegalAccessException
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(e, fooException);
      addSuppressed(e, barException);
      ex = e;
    }
    verify(ex);
  }

  @Test
  public void suppressedWithCause() throws InvocationTargetException, IllegalAccessException
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      ex=new Exception("Wrapper", e);
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(ex, fooException);
      addSuppressed(e, barException);
    }
    verify(ex);
  }

  @Test
  public void suppressedWithSuppressed() throws Exception
  {
    assumeTrue(TestHelper.suppressedSupported()); // only execute on Java 7, would work anyway but doesn't make sense.
    Exception ex = null;
    try {
      someMethod();
    } catch (Exception e) {
      ex=new Exception("Wrapper", e);
      Exception fooException = new Exception("Foo");
      Exception barException = new Exception("Bar");
      addSuppressed(barException, fooException);
      addSuppressed(e, barException);
    }
    verify(ex);
  }

  // see also http://jira.qos.ch/browse/LBCLASSIC-216
  @Test
  public void nullSTE() {
    Throwable t = new Exception("someMethodWithNullException") {
      private static final long serialVersionUID = 1L;

      @Override
      public StackTraceElement[] getStackTrace() {
        return null;
      }
    };
    // we can't test output as Throwable.printStackTrace method uses
    // the private getOurStackTrace method instead of getStackTrace

    // tests  ThrowableProxyUtil.steArrayToStepArray
    new ThrowableProxy(t);

    // tests  ThrowableProxyUtil.findNumberOfCommonFrames
    Exception top = new Exception("top", t);
    new ThrowableProxy(top);
  }

  @Test
  public void multiNested() {
    Exception w = null;
    try {
      someOtherMethod();
    } catch (Exception e) {
      w = new Exception("wrapping", e);
    }
    verify(w);
  }

  @Test
  public void circularCause() {
    Exception ex1 = new Exception("Foo");
    Exception ex2 = new Exception("Bar");

    ex1.initCause(ex2);
    ex2.initCause(ex1);

    verifyContains(ex1, "Caused by: CIRCULAR REFERENCE:java.lang.Exception: Foo");
    verifyContains(ex2, "Caused by: CIRCULAR REFERENCE:java.lang.Exception: Bar");
  }

  @Test
  public void circularSuppressed() {
    Exception ex1 = new Exception("Foo");
    Exception ex2 = new Exception("Bar");

    ex1.addSuppressed(ex2);
    ex2.addSuppressed(ex1);

    verifyContains(ex1, "Suppressed: CIRCULAR REFERENCE:java.lang.Exception: Foo");
    verifyContains(ex2, "Suppressed: CIRCULAR REFERENCE:java.lang.Exception: Bar");
  }

  void someMethod() throws Exception {
    throw new Exception("someMethod");
  }

  void someMethodWithNullException() throws Exception {
    throw new Exception("someMethodWithNullException") {
      private static final long serialVersionUID = -2419053636101615373L;

      @Override
      public StackTraceElement[] getStackTrace() {
        return null;
      }
    };
  }

  void someOtherMethod() throws Exception {
    try {
      someMethod();
    } catch (Exception e) {
      throw new Exception("someOtherMethod", e);
    }
  }
}
