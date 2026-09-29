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
package ch.qos.logback.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Unit tests for {@link PropertySetterException}.
 */
public class PropertySetterExceptionTest {

  @Test
  public void messageOnly() {
    PropertySetterException e = new PropertySetterException("bad property");
    assertEquals("bad property", e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  public void rootCauseOnly() {
    Throwable cause = new IllegalStateException("root");
    PropertySetterException e = new PropertySetterException(cause);
    assertSame(cause, e.getCause());
    assertEquals(cause.toString(), e.getMessage());
  }

  @Test
  public void messageAndCause() {
    Throwable cause = new IllegalStateException("root");
    PropertySetterException e = new PropertySetterException("bad property", cause);
    assertEquals("bad property", e.getMessage());
    assertSame(cause, e.getCause());
  }
}
