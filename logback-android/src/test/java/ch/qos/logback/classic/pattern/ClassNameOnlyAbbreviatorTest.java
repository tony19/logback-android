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
package ch.qos.logback.classic.pattern;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClassNameOnlyAbbreviatorTest {

  private final ClassNameOnlyAbbreviator abbreviator = new ClassNameOnlyAbbreviator();

  @Test
  public void stripsPackageFromQualifiedName() {
    assertEquals("Foobar", abbreviator.abbreviate("com.logback.Foobar"));
  }

  @Test
  public void keepsInnerClassSeparator() {
    assertEquals("Foobar$Inner", abbreviator.abbreviate("com.logback.Foobar$Inner"));
  }

  @Test
  public void returnsNameWithoutPackageUnchanged() {
    assertEquals("Foobar", abbreviator.abbreviate("Foobar"));
  }

  @Test
  public void nameEndingWithDotAbbreviatesToEmpty() {
    assertEquals("", abbreviator.abbreviate("com.logback."));
  }
}
