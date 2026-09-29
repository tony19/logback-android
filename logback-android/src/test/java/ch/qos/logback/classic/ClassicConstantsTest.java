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
package ch.qos.logback.classic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.slf4j.MarkerFactory;

public class ClassicConstantsTest {

  @Test
  public void finalizeSessionMarkerIsTheSharedMarkerNamedFinalizeSession() {
    assertEquals("FINALIZE_SESSION", ClassicConstants.FINALIZE_SESSION);
    assertEquals(ClassicConstants.FINALIZE_SESSION, ClassicConstants.FINALIZE_SESSION_MARKER.getName());
    assertSame(MarkerFactory.getMarker(ClassicConstants.FINALIZE_SESSION), ClassicConstants.FINALIZE_SESSION_MARKER);
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class exposes an implicit public constructor (kept for API compatibility)
    assertNotNull(new ClassicConstants());
  }
}
