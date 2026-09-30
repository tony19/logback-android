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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThrowableProxyVOTest {

  /** A new String instance, equal to but not the same as {@code s}. */
  static String copyOf(String s) {
    return new StringBuilder(s).toString();
  }

  static StackTraceElementProxy[] stackTrace(String method) {
    return new StackTraceElementProxy[] {
        new StackTraceElementProxy(new StackTraceElement("a.b.C", method, "C.java", 10)) };
  }

  /** A proxy with a class name, message, common frames and a one-frame stack trace; no cause, no suppressed. */
  static DummyThrowableProxy proxy(String className) {
    DummyThrowableProxy proxy = new DummyThrowableProxy();
    proxy.setClassName(className == null ? null : copyOf(className));
    proxy.setMessage("message of " + className);
    proxy.setCommonFramesCount(3);
    proxy.setStackTraceElementProxyArray(stackTrace("run"));
    return proxy;
  }

  static DummyThrowableProxy proxyWithCause(String className, IThrowableProxy cause) {
    DummyThrowableProxy proxy = proxy(className);
    proxy.setCause(cause);
    return proxy;
  }

  static DummyThrowableProxy proxyWithSuppressed(String className, IThrowableProxy... suppressed) {
    DummyThrowableProxy proxy = proxy(className);
    proxy.setSuppressed(suppressed);
    return proxy;
  }

  @Test
  public void buildOfNullIsNull() {
    assertNull(ThrowableProxyVO.build(null));
  }

  @Test
  public void buildCopiesTheStateOfTheSourceProxy() {
    DummyThrowableProxy source = proxy("java.lang.Exception");

    ThrowableProxyVO vo = ThrowableProxyVO.build(source);

    assertEquals("java.lang.Exception", vo.getClassName());
    assertEquals("message of java.lang.Exception", vo.getMessage());
    assertEquals(3, vo.getCommonFrames());
    assertSame(source.getStackTraceElementProxyArray(), vo.getStackTraceElementProxyArray());
    assertNull(vo.getCause());
    assertNull(vo.getSuppressed());
  }

  @Test
  public void buildConvertsTheWholeCauseChain() {
    DummyThrowableProxy root = proxy("java.io.IOException");
    DummyThrowableProxy middle = proxyWithCause("java.lang.IllegalStateException", root);
    DummyThrowableProxy top = proxyWithCause("java.lang.RuntimeException", middle);

    ThrowableProxyVO vo = ThrowableProxyVO.build(top);

    IThrowableProxy cause = vo.getCause();
    assertTrue(cause instanceof ThrowableProxyVO);
    assertEquals("java.lang.IllegalStateException", cause.getClassName());
    assertEquals("message of java.lang.IllegalStateException", cause.getMessage());

    IThrowableProxy rootCause = cause.getCause();
    assertTrue(rootCause instanceof ThrowableProxyVO);
    assertEquals("java.io.IOException", rootCause.getClassName());
    assertNull(rootCause.getCause());
  }

  @Test
  public void buildConvertsEverySuppressedProxyInOrder() {
    DummyThrowableProxy first = proxy("java.lang.IllegalArgumentException");
    DummyThrowableProxy second = proxyWithCause("java.lang.IllegalStateException", proxy("java.io.IOException"));
    DummyThrowableProxy source = proxyWithSuppressed("java.lang.Exception", first, second);

    ThrowableProxyVO vo = ThrowableProxyVO.build(source);

    IThrowableProxy[] suppressed = vo.getSuppressed();
    assertEquals(2, suppressed.length);
    assertNotSame(source.getSuppressed(), suppressed);
    assertTrue(suppressed[0] instanceof ThrowableProxyVO);
    assertEquals("java.lang.IllegalArgumentException", suppressed[0].getClassName());
    assertTrue(suppressed[1] instanceof ThrowableProxyVO);
    assertEquals("java.lang.IllegalStateException", suppressed[1].getClassName());
    assertEquals("java.io.IOException", suppressed[1].getCause().getClassName());
  }

  @Test
  public void buildKeepsAnEmptySuppressedArrayEmpty() {
    ThrowableProxyVO vo = ThrowableProxyVO.build(proxyWithSuppressed("java.lang.Exception"));

    assertEquals(0, vo.getSuppressed().length);
  }

  @Test
  public void hashCodeDependsOnTheClassName() {
    ThrowableProxyVO vo = ThrowableProxyVO.build(proxy("java.lang.Exception"));

    assertEquals(31 + "java.lang.Exception".hashCode(), vo.hashCode());
    assertNotEquals(vo.hashCode(), ThrowableProxyVO.build(proxy("java.lang.Error")).hashCode());
  }

  @Test
  public void hashCodeOfNullClassNameIsConstant() {
    ThrowableProxyVO vo = ThrowableProxyVO.build(proxy(null));
    assertEquals(31, vo.hashCode());
  }

  @Test
  public void equalsIsReflexive() {
    ThrowableProxyVO vo = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    assertTrue(vo.equals(vo));
  }

  @Test
  public void equalsRejectsNullAndOtherTypes() {
    DummyThrowableProxy source = proxy("java.lang.Exception");
    ThrowableProxyVO vo = ThrowableProxyVO.build(source);

    assertFalse(vo.equals(null));
    assertFalse(vo.equals(source));
    assertFalse(vo.equals("java.lang.Exception"));
  }

  @Test
  public void equalsRejectsSubclassInstancesWithTheSameState() {
    ThrowableProxyVO plain = ThrowableProxyVO.build(new DummyThrowableProxy());
    ThrowableProxyVO subclass = new ThrowableProxyVO() {
      private static final long serialVersionUID = 1L;
    };

    assertFalse(plain.equals(subclass));
    assertFalse(subclass.equals(plain));
  }

  @Test
  public void equalsHoldsForDistinctProxiesWithEqualState() {
    ThrowableProxyVO a = ThrowableProxyVO.build(proxyWithSuppressed("java.lang.Exception", proxy("java.lang.Error")));
    ThrowableProxyVO b = ThrowableProxyVO.build(proxyWithSuppressed("java.lang.Exception", proxy("java.lang.Error")));
    assertNotSame(a.getClassName(), b.getClassName());
    assertNotSame(a.getStackTraceElementProxyArray(), b.getStackTraceElementProxyArray());

    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void equalsHoldsWhenClassNamesAreNullOnBothSides() {
    ThrowableProxyVO a = ThrowableProxyVO.build(proxy(null));
    ThrowableProxyVO b = ThrowableProxyVO.build(proxy(null));
    assertTrue(a.equals(b));
  }

  @Test
  public void equalsComparesClassNames() {
    ThrowableProxyVO exception = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    ThrowableProxyVO error = ThrowableProxyVO.build(proxy("java.lang.Error"));
    ThrowableProxyVO unnamed = ThrowableProxyVO.build(proxy(null));

    assertFalse(exception.equals(error));
    assertFalse(exception.equals(unnamed));
    assertFalse(unnamed.equals(exception));
  }

  @Test
  public void equalsComparesStackTraces() {
    DummyThrowableProxy other = proxy("java.lang.Exception");
    other.setStackTraceElementProxyArray(stackTrace("call"));

    ThrowableProxyVO a = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    ThrowableProxyVO b = ThrowableProxyVO.build(other);

    assertFalse(a.equals(b));
    assertFalse(b.equals(a));
  }

  @Test
  public void equalsComparesSuppressedProxies() {
    ThrowableProxyVO none = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    ThrowableProxyVO withError = ThrowableProxyVO.build(proxyWithSuppressed("java.lang.Exception", proxy("java.lang.Error")));
    ThrowableProxyVO withOther = ThrowableProxyVO.build(proxyWithSuppressed("java.lang.Exception", proxy("java.io.IOException")));

    assertFalse(none.equals(withError));
    assertFalse(withError.equals(none));
    assertFalse(withError.equals(withOther));
  }

  @Test
  public void equalsComparesCauses() {
    ThrowableProxyVO noCause = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    ThrowableProxyVO alsoNoCause = ThrowableProxyVO.build(proxy("java.lang.Exception"));
    ThrowableProxyVO ioCause = ThrowableProxyVO.build(proxyWithCause("java.lang.Exception", proxy("java.io.IOException")));
    ThrowableProxyVO sameIoCause = ThrowableProxyVO.build(proxyWithCause("java.lang.Exception", proxy("java.io.IOException")));
    ThrowableProxyVO errorCause = ThrowableProxyVO.build(proxyWithCause("java.lang.Exception", proxy("java.lang.Error")));

    assertTrue(noCause.equals(alsoNoCause));
    assertTrue(ioCause.equals(sameIoCause));
    assertFalse(noCause.equals(ioCause));
    assertFalse(ioCause.equals(noCause));
    assertFalse(ioCause.equals(errorCause));
  }
}
