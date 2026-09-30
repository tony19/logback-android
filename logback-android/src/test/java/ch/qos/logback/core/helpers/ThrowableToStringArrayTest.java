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
package ch.qos.logback.core.helpers;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.CoreConstants;

public class ThrowableToStringArrayTest {

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
    
    String[] sa = ThrowableToStringArray.convert(t);
    StringBuilder sb = new StringBuilder();
    for (String tdp : sa) {
      sb.append(tdp);
      sb.append(CoreConstants.LINE_SEPARATOR);
    }
    String expected = sw.toString();
    String result = sb.toString().replace("common frames omitted", "more");
    assertEquals(expected, result);
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
  public void multiNested() {
    Exception w = null;
    try {
      someOtherMethod();
    } catch (Exception e) {
      w = new Exception("wrapping", e);
    }
    verify(w);
  }
  
  private static StackTraceElement frame(String method) {
    return new StackTraceElement("com.example.Foo", method, "Foo.java", 1);
  }

  private static String at(StackTraceElement ste) {
    return "\tat " + ste;
  }

  @Test
  public void publicNoArgConstructorIsAvailable() throws Exception {
    // the class only has static methods, but its implicit public constructor is part of its API
    assertNotNull(ThrowableToStringArray.class.getConstructor().newInstance());
  }

  @Test
  public void throwableWithoutMessageShowsOnlyItsClassName() {
    StackTraceElement a = frame("a");
    Exception e = new Exception();
    e.setStackTrace(new StackTraceElement[] { a });

    assertArrayEquals(new String[] { "java.lang.Exception", at(a) }, ThrowableToStringArray.convert(e));
    verify(e);
  }

  @Test
  public void causeWithoutMessageShowsOnlyItsClassName() {
    StackTraceElement a = frame("a");
    StackTraceElement b = frame("b");
    IllegalStateException cause = new IllegalStateException();
    cause.setStackTrace(new StackTraceElement[] { b });
    Exception e = new Exception("wrapper", cause);
    e.setStackTrace(new StackTraceElement[] { a });

    assertArrayEquals(new String[] { "java.lang.Exception: wrapper", at(a),
        CoreConstants.CAUSED_BY + "java.lang.IllegalStateException", at(b) }, ThrowableToStringArray.convert(e));
  }

  @Test
  public void causeWhoseFramesAreAllSharedWithTheParentShowsNoFrame() {
    StackTraceElement top = frame("top");
    StackTraceElement mid = frame("mid");
    StackTraceElement bottom = frame("bottom");
    Exception cause = new Exception("cause");
    cause.setStackTrace(new StackTraceElement[] { mid, bottom });
    Exception e = new Exception("wrapper", cause);
    e.setStackTrace(new StackTraceElement[] { top, mid, bottom });

    assertArrayEquals(new String[] { "java.lang.Exception: wrapper", at(top), at(mid), at(bottom),
        CoreConstants.CAUSED_BY + "java.lang.Exception: cause", "\t... 2 common frames omitted" },
        ThrowableToStringArray.convert(e));
    verify(e);
  }

  @Test
  public void causeWithMoreFramesThanTheParentShowsOnlyItsOwnFrames() {
    StackTraceElement own0 = frame("own0");
    StackTraceElement own1 = frame("own1");
    StackTraceElement bottom = frame("bottom");
    Exception cause = new Exception("cause");
    cause.setStackTrace(new StackTraceElement[] { own0, own1, bottom });
    Exception e = new Exception("wrapper", cause);
    e.setStackTrace(new StackTraceElement[] { bottom });

    assertArrayEquals(new String[] { "java.lang.Exception: wrapper", at(bottom),
        CoreConstants.CAUSED_BY + "java.lang.Exception: cause", at(own0), at(own1), "\t... 1 common frames omitted" },
        ThrowableToStringArray.convert(e));
    verify(e);
  }

  void someMethod() throws Exception {
    throw new Exception("someMethod");
  }

  void someOtherMethod() throws Exception {
    try {
      someMethod();
    } catch (Exception e) {
      throw new Exception("someOtherMethod", e);
    }
  }
}
