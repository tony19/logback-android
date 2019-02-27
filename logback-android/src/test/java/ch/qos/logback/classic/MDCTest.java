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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.MDC;

public class MDCTest {

  @Test
  public void test() throws InterruptedException {
    MDCTestThread threadA = new MDCTestThread("a");
    threadA.start();

    MDCTestThread threadB = new MDCTestThread("b");
    threadB.start();

    threadA.join();
    threadB.join();

    assertNull(threadA.x0);
    assertEquals("a", threadA.x1);
    assertNull(threadA.x2);

    assertNull(threadB.x0);
    assertEquals("b", threadB.x1);
    assertNull(threadB.x2);

  }

  @Test
  public void testLBCLASSIC_98() {
    MDC.setContextMap(new HashMap<String, String>());
  }
  
}
