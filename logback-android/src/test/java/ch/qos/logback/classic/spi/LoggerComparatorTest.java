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

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;


public class LoggerComparatorTest {

  LoggerComparator comparator = new LoggerComparator();
  LoggerContext lc = new LoggerContext();

  Logger root = lc.getLogger("root");

  Logger a = lc.getLogger("a");
  Logger b = lc.getLogger("b");

  @Before
  public void setUp() throws Exception {
  
  }

  
  
  @Test
  public void testSmoke() {
    assertEquals(0, comparator.compare(a, a));
    assertEquals(-1, comparator.compare(a, b));
    assertEquals(1, comparator.compare(b, a));
    assertEquals(-1, comparator.compare(root, a));
    // following two tests failed before bug #127 was fixed
    assertEquals(1, comparator.compare(a, root));
    assertEquals(0, comparator.compare(root, root));
  }
}
