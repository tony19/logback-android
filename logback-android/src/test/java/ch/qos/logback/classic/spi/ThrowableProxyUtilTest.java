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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ch.qos.logback.core.CoreConstants;

public class ThrowableProxyUtilTest {

  private static final String LS = CoreConstants.LINE_SEPARATOR;

  private static final StackTraceElement A = ste("A");
  private static final StackTraceElement B = ste("B");
  private static final StackTraceElement C = ste("C");
  private static final StackTraceElement X = ste("X");

  @Test
  public void buildWithParentCountsFramesCommonWithTheParent() {
    ThrowableProxy parentTP = new ThrowableProxy(throwableWithFrames("parent", A, B, C));
    Throwable nested = throwableWithFrames("nested", X, B, C);
    ThrowableProxy nestedTP = new ThrowableProxy(throwableWithFrames("placeholder", A));

    ThrowableProxyUtil.build(nestedTP, nested, parentTP);

    assertEquals(2, nestedTP.getCommonFrames());
    assertArrayEquals(steps(X, B, C), nestedTP.getStackTraceElementProxyArray());
  }

  @Test
  public void buildWithoutParentMarksCommonFramesAsUnknown() {
    Throwable nested = throwableWithFrames("nested", X, B);
    ThrowableProxy nestedTP = new ThrowableProxy(throwableWithFrames("placeholder", A));

    ThrowableProxyUtil.build(nestedTP, nested, null);

    assertEquals(-1, nestedTP.getCommonFrames());
    assertArrayEquals(steps(X, B), nestedTP.getStackTraceElementProxyArray());
  }

  @Test
  public void noCommonFramesWhenEitherArrayIsMissing() {
    assertEquals(0, ThrowableProxyUtil.findNumberOfCommonFrames(null, steps(A)));
    assertEquals(0, ThrowableProxyUtil.findNumberOfCommonFrames(new StackTraceElement[] {A}, null));
  }

  @Test
  public void commonFrameCountStopsWhenEitherArrayIsExhausted() {
    assertEquals(2, ThrowableProxyUtil.findNumberOfCommonFrames(new StackTraceElement[] {A, B, C}, steps(B, C)));
    assertEquals(2, ThrowableProxyUtil.findNumberOfCommonFrames(new StackTraceElement[] {B, C}, steps(A, B, C)));
    assertEquals(0, ThrowableProxyUtil.findNumberOfCommonFrames(new StackTraceElement[] {A, B}, steps(A, C)));
  }

  @Test
  public void asStringToleratesProxyWithoutSuppressedArray() {
    DummyThrowableProxy tp = new DummyThrowableProxy();
    tp.setClassName("com.acme.BoomException");
    tp.setMessage("boom");
    tp.setStackTraceElementProxyArray(steps(A));
    tp.setSuppressed(null);

    assertEquals("com.acme.BoomException: boom" + LS + "\tat A.method(A.java:1)" + LS,
        ThrowableProxyUtil.asString(tp));
  }

  @Test
  public void noPackagingDataIsAppendedForNullStep() {
    StringBuilder sb = new StringBuilder("x");
    ThrowableProxyUtil.subjoinPackagingData(sb, null);
    assertEquals("x", sb.toString());
  }

  @Test
  public void noPackagingDataIsAppendedForStepWithoutPackagingData() {
    StringBuilder sb = new StringBuilder("x");
    ThrowableProxyUtil.subjoinPackagingData(sb, new StackTraceElementProxy(A));
    assertEquals("x", sb.toString());
  }

  @Test
  public void exactPackagingDataIsAppendedInBrackets() {
    StackTraceElementProxy step = new StackTraceElementProxy(A);
    step.setClassPackagingData(new ClassPackagingData("foo.jar", "1.0", true));

    StringBuilder sb = new StringBuilder();
    ThrowableProxyUtil.subjoinPackagingData(sb, step);

    assertEquals(" [foo.jar:1.0]", sb.toString());
  }

  @Test
  public void inexactPackagingDataIsAppendedWithTilde() {
    StackTraceElementProxy step = new StackTraceElementProxy(A);
    step.setClassPackagingData(new ClassPackagingData("foo.jar", "1.0", false));

    StringBuilder sb = new StringBuilder();
    ThrowableProxyUtil.subjoinPackagingData(sb, step);

    assertEquals(" ~[foo.jar:1.0]", sb.toString());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void deprecatedSubjoinSTEPArrayUsesTheRegularIndent() {
    DummyThrowableProxy tp = new DummyThrowableProxy();
    tp.setStackTraceElementProxyArray(steps(A, B, C));
    tp.setCommonFramesCount(1);

    StringBuilder deprecated = new StringBuilder();
    ThrowableProxyUtil.subjoinSTEPArray(deprecated, tp);

    assertEquals("\tat A.method(A.java:1)" + LS
        + "\tat B.method(B.java:1)" + LS
        + "\t... 1 common frames omitted" + LS, deprecated.toString());
    StringBuilder current = new StringBuilder();
    ThrowableProxyUtil.subjoinSTEPArray(current, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, tp);
    assertEquals(current.toString(), deprecated.toString());
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class only has static members, but its implicit public constructor is API
    assertNotNull(new ThrowableProxyUtil());
  }

  private static StackTraceElement ste(String className) {
    return new StackTraceElement(className, "method", className + ".java", 1);
  }

  private static StackTraceElementProxy[] steps(StackTraceElement... stes) {
    return ThrowableProxyUtil.steArrayToStepArray(stes);
  }

  private static Throwable throwableWithFrames(String message, StackTraceElement... frames) {
    Throwable t = new Exception(message);
    t.setStackTrace(frames);
    return t;
  }
}
