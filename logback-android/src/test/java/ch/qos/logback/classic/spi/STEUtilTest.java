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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class STEUtilTest {

  private static final StackTraceElement A = ste("A");
  private static final StackTraceElement B = ste("B");
  private static final StackTraceElement C = ste("C");
  private static final StackTraceElement X = ste("X");

  @Test
  public void noCommonFramesWithoutOtherArray() {
    assertEquals(0, STEUtil.findNumberOfCommonFrames(new StackTraceElement[] {A, B}, null));
  }

  @Test
  public void commonFramesAreCountedFromTheBottomUntilTheFirstDifference() {
    assertEquals(2, STEUtil.findNumberOfCommonFrames(new StackTraceElement[] {X, B, C}, steps(A, B, C)));
    assertEquals(0, STEUtil.findNumberOfCommonFrames(new StackTraceElement[] {A, B, X}, steps(A, B, C)));
  }

  @Test
  public void countStopsWhenTheOtherArrayIsExhausted() {
    assertEquals(2, STEUtil.findNumberOfCommonFrames(new StackTraceElement[] {A, B, C}, steps(B, C)));
  }

  @Test
  public void countStopsWhenTheFirstArrayIsExhausted() {
    assertEquals(2, STEUtil.findNumberOfCommonFrames(new StackTraceElement[] {B, C}, steps(A, B, C)));
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class only has static members, but its implicit public constructor is API
    assertNotNull(new STEUtil());
  }

  private static StackTraceElement ste(String className) {
    return new StackTraceElement(className, "method", className + ".java", 1);
  }

  private static StackTraceElementProxy[] steps(StackTraceElement... stes) {
    StackTraceElementProxy[] steps = new StackTraceElementProxy[stes.length];
    for (int i = 0; i < stes.length; i++) {
      steps[i] = new StackTraceElementProxy(stes[i]);
    }
    return steps;
  }
}
