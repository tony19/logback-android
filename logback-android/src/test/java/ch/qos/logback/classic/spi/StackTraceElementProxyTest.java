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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StackTraceElementProxyTest {

  private final StackTraceElement ste = new StackTraceElement("com.acme.Foo", "bar", "Foo.java", 12);
  private final StackTraceElement otherSte = new StackTraceElement("com.acme.Foo", "baz", "Foo.java", 34);

  @Test
  public void nullStackTraceElementIsRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> new StackTraceElementProxy(null));
    assertEquals("ste cannot be null", e.getMessage());
  }

  @Test
  public void stringFormIsTheElementPrefixedWithAt() {
    StackTraceElementProxy step = new StackTraceElementProxy(ste);

    assertEquals("at com.acme.Foo.bar(Foo.java:12)", step.getSTEAsString());
    assertSame(step.getSTEAsString(), step.getSTEAsString());
    assertEquals(step.getSTEAsString(), step.toString());
  }

  @Test
  public void hashCodeIsTheElementHashCode() {
    assertEquals(ste.hashCode(), new StackTraceElementProxy(ste).hashCode());
  }

  @Test
  public void classPackagingDataCanBeSetOnlyOnce() {
    StackTraceElementProxy step = new StackTraceElementProxy(ste);
    ClassPackagingData first = new ClassPackagingData("foo.jar", "1.0");
    step.setClassPackagingData(first);

    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> step.setClassPackagingData(new ClassPackagingData("bar.jar", "2.0")));
    assertEquals("Packaging data has been already set", e.getMessage());
    assertSame(first, step.getClassPackagingData());
  }

  @Test
  public void equalsIsReflexive() {
    StackTraceElementProxy step = new StackTraceElementProxy(ste);
    assertTrue(step.equals(step));
  }

  @Test
  public void notEqualToNullOrAnotherClass() {
    StackTraceElementProxy step = new StackTraceElementProxy(ste);
    assertFalse(step.equals(null));
    assertFalse(step.equals(ste));
  }

  @Test
  public void stackTraceElementIsCompared() {
    assertNotEquals(new StackTraceElementProxy(ste), new StackTraceElementProxy(otherSte));
  }

  @Test
  public void equalWithSameElementAndNoPackagingData() {
    assertEquals(new StackTraceElementProxy(ste), new StackTraceElementProxy(ste));
  }

  @Test
  public void classPackagingDataIsCompared() {
    StackTraceElementProxy withoutCpd = new StackTraceElementProxy(ste);
    StackTraceElementProxy withFoo = withCpd(new ClassPackagingData("foo.jar", "1.0"));
    StackTraceElementProxy withOtherFoo = withCpd(new ClassPackagingData("foo.jar", "1.0"));
    StackTraceElementProxy withBar = withCpd(new ClassPackagingData("bar.jar", "1.0"));

    assertNotEquals(withoutCpd, withFoo);
    assertNotEquals(withFoo, withoutCpd);
    assertNotEquals(withFoo, withBar);
    assertEquals(withFoo, withOtherFoo);
  }

  private StackTraceElementProxy withCpd(ClassPackagingData cpd) {
    StackTraceElementProxy step = new StackTraceElementProxy(ste);
    step.setClassPackagingData(cpd);
    return step;
  }
}
