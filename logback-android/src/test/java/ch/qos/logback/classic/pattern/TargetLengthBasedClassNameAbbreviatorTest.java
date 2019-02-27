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

import static junit.framework.Assert.*;

import org.junit.Test;

import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;

public class TargetLengthBasedClassNameAbbreviatorTest  {


  @Test
  public void testShortName() {
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(100);
      String name = "hello";
      assertEquals(name, abbreviator.abbreviate(name));
    }
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(100);
      String name = "hello.world";
      assertEquals(name, abbreviator.abbreviate(name));
    }
  }

  @Test
  public void testNoDot() {
    TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
    String name = "hello";
    assertEquals(name, abbreviator.abbreviate(name));
  }

  @Test
  public void testOneDot() {
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "hello.world";
      assertEquals("h.world", abbreviator.abbreviate(name));
    }

    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "h.world";
      assertEquals("h.world", abbreviator.abbreviate(name));
    }

    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = ".world";
      assertEquals(".world", abbreviator.abbreviate(name));
    }
  }

  @Test
  public void testTwoDot() {
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "com.logback.Foobar";
      assertEquals("c.l.Foobar", abbreviator.abbreviate(name));
    }

    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "c.logback.Foobar";
      assertEquals("c.l.Foobar", abbreviator.abbreviate(name));
    }

    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "c..Foobar";
      assertEquals("c..Foobar", abbreviator.abbreviate(name));
    }
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "..Foobar";
      assertEquals("..Foobar", abbreviator.abbreviate(name));
    }
  }
  
  @Test
  public void test3Dot() {
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "com.logback.xyz.Foobar";
      assertEquals("c.l.x.Foobar", abbreviator.abbreviate(name));
    }
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(13);
      String name = "com.logback.xyz.Foobar";
      assertEquals("c.l.x.Foobar", abbreviator.abbreviate(name));
    }
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(14);
      String name = "com.logback.xyz.Foobar";
      assertEquals("c.l.xyz.Foobar", abbreviator.abbreviate(name));
    }
    
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(15);
      String name = "com.logback.alligator.Foobar";
      assertEquals("c.l.a.Foobar", abbreviator.abbreviate(name));
    }
  }
  @Test
  public void testXDot() {
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(21);
      String name = "com.logback.wombat.alligator.Foobar";
      assertEquals("c.l.w.a.Foobar", abbreviator.abbreviate(name));
    }
    
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(22);
      String name = "com.logback.wombat.alligator.Foobar";
      assertEquals("c.l.w.alligator.Foobar", abbreviator.abbreviate(name));
    }
    
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(1);
      String name = "com.logback.wombat.alligator.tomato.Foobar";
      assertEquals("c.l.w.a.t.Foobar", abbreviator.abbreviate(name));
    }
    
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(21);
      String name = "com.logback.wombat.alligator.tomato.Foobar";
      assertEquals("c.l.w.a.tomato.Foobar", abbreviator.abbreviate(name));
    }
    
    {
      TargetLengthBasedClassNameAbbreviator abbreviator = new TargetLengthBasedClassNameAbbreviator(29);
      String name = "com.logback.wombat.alligator.tomato.Foobar";
      assertEquals("c.l.w.alligator.tomato.Foobar", abbreviator.abbreviate(name));
    }
  }
}
