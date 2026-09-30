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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClassPackagingDataTest {

  @Test
  public void twoArgConstructorCreatesExactData() {
    ClassPackagingData cpd = new ClassPackagingData("foo.jar", "1.2");

    assertEquals("foo.jar", cpd.getCodeLocation());
    assertEquals("1.2", cpd.getVersion());
    assertTrue(cpd.isExact());
  }

  @Test
  public void threeArgConstructorKeepsExactness() {
    ClassPackagingData cpd = new ClassPackagingData("foo.jar", "1.2", false);

    assertEquals("foo.jar", cpd.getCodeLocation());
    assertEquals("1.2", cpd.getVersion());
    assertFalse(cpd.isExact());
  }

  @Test
  public void hashCodeDependsOnlyOnCodeLocation() {
    assertEquals(31, new ClassPackagingData(null, "1.2").hashCode());
    assertEquals(31 + "foo.jar".hashCode(), new ClassPackagingData("foo.jar", "1.2").hashCode());
    assertEquals(new ClassPackagingData("foo.jar", "1.2", true).hashCode(),
        new ClassPackagingData("foo.jar", "3.4", false).hashCode());
  }

  @Test
  public void equalsIsReflexive() {
    ClassPackagingData cpd = new ClassPackagingData("foo.jar", "1.2");
    assertTrue(cpd.equals(cpd));
  }

  @Test
  public void notEqualToNull() {
    assertFalse(new ClassPackagingData("foo.jar", "1.2").equals(null));
  }

  @Test
  public void notEqualToInstanceOfAnotherClass() {
    ClassPackagingData subclassInstance = new ClassPackagingData("foo.jar", "1.2") {
      private static final long serialVersionUID = 1L;
    };
    assertFalse(new ClassPackagingData("foo.jar", "1.2").equals(subclassInstance));
    assertFalse(new ClassPackagingData("foo.jar", "1.2").equals("foo.jar"));
  }

  @Test
  public void equalWhenAllFieldsAreEqual() {
    assertEquals(new ClassPackagingData("foo.jar", "1.2", false), new ClassPackagingData("foo.jar", "1.2", false));
    assertEquals(new ClassPackagingData(null, null), new ClassPackagingData(null, null));
  }

  @Test
  public void codeLocationIsCompared() {
    assertNotEquals(new ClassPackagingData(null, "1.2"), new ClassPackagingData("foo.jar", "1.2"));
    assertNotEquals(new ClassPackagingData("foo.jar", "1.2"), new ClassPackagingData(null, "1.2"));
    assertNotEquals(new ClassPackagingData("foo.jar", "1.2"), new ClassPackagingData("bar.jar", "1.2"));
  }

  @Test
  public void exactnessIsCompared() {
    assertNotEquals(new ClassPackagingData("foo.jar", "1.2", true), new ClassPackagingData("foo.jar", "1.2", false));
  }

  @Test
  public void versionIsCompared() {
    assertNotEquals(new ClassPackagingData("foo.jar", null), new ClassPackagingData("foo.jar", "1.2"));
    assertNotEquals(new ClassPackagingData("foo.jar", "1.2"), new ClassPackagingData("foo.jar", null));
    assertNotEquals(new ClassPackagingData("foo.jar", "1.2"), new ClassPackagingData("foo.jar", "3.4"));
  }
}
