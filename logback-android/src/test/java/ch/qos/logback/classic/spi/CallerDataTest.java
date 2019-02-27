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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class CallerDataTest  {


  @Test
  public void testBasic() {
    Throwable t = new Throwable();
    StackTraceElement[] steArray = t.getStackTrace();
    
    StackTraceElement[] cda = CallerData.extract(t, CallerDataTest.class.getName(), 50, null);
    assertNotNull(cda);
    assertTrue(cda.length > 0);
    assertEquals(steArray.length - 1, cda.length);
  }
  
  /**
   * This test verifies that in case caller data cannot be
   * extracted, CallerData.extract does not throw an exception
   *
   */
  @Test
  public void testDeferredProcessing() {
    StackTraceElement[] cda = CallerData.extract(new Throwable(), "com.inexistent.foo", 10, null);
    assertNotNull(cda);
    assertEquals(0, cda.length);
  }
  
}
