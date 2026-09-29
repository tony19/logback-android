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
package ch.qos.logback.core.joran.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class JoranExceptionTest {

  @Test
  public void messageOnlyConstructorHasNoCause() {
    JoranException e = new JoranException("bad config");
    assertEquals("bad config", e.getMessage());
    assertNull(e.getCause());
  }

  @Test
  public void causeIsKept() {
    Throwable cause = new IllegalStateException("root");
    JoranException e = new JoranException("bad config", cause);
    assertEquals("bad config", e.getMessage());
    assertSame(cause, e.getCause());
  }
}
