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
package ch.qos.logback.core.pattern;

import static junit.framework.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpacePadderTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void smoke() {
    {
      StringBuilder buf = new StringBuilder();
      String s = "a";
      SpacePadder.leftPad(buf, s, 4);
      assertEquals("   a", buf.toString());
    }
    {
      StringBuilder buf = new StringBuilder();
      String s = "a";
      SpacePadder.rightPad(buf, s, 4);
      assertEquals("a   ", buf.toString());
    }
  }

  @Test
  public void nullString() {
    String s = null;
    {
      StringBuilder buf = new StringBuilder();
      SpacePadder.leftPad(buf, s, 2);
      assertEquals("  ", buf.toString());
    }
    {
      StringBuilder buf = new StringBuilder();
      SpacePadder.rightPad(buf, s, 2);
      assertEquals("  ", buf.toString());
    }
  }

  @Test
  public void longString() {
    {
      StringBuilder buf = new StringBuilder();
      String s = "abc";
      SpacePadder.leftPad(buf, s, 2);
      assertEquals(s, buf.toString());
    }

    {
      StringBuilder buf = new StringBuilder();
      String s = "abc";
      SpacePadder.rightPad(buf, s, 2);
      assertEquals(s, buf.toString());
    }
  }
  
  @Test
  public void lengthyPad() {
    {
      StringBuilder buf = new StringBuilder();
      String s = "abc";
      SpacePadder.leftPad(buf, s, 33);
      assertEquals("                              abc", buf.toString());
    }
    {
      StringBuilder buf = new StringBuilder();
      String s = "abc";
      SpacePadder.rightPad(buf, s, 33);
      assertEquals("abc                              ", buf.toString());
    }
    
  }

}
