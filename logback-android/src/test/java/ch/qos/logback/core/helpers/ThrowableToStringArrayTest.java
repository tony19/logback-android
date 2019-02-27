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
package ch.qos.logback.core.helpers;

import static junit.framework.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.CoreConstants;

public class ThrowableToStringArrayTest {

  StringWriter sw = new StringWriter();
  PrintWriter pw = new PrintWriter(sw);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  public void verify(Throwable t) {
    t.printStackTrace(pw);
    
    String[] sa = ThrowableToStringArray.convert(t);
    StringBuilder sb = new StringBuilder();
    for (String tdp : sa) {
      sb.append(tdp);
      sb.append(CoreConstants.LINE_SEPARATOR);
    }
    String expected = sw.toString();
    String result = sb.toString().replace("common frames omitted", "more");
    assertEquals(expected, result);
  }
  
  @Test
  public void smoke() {
    Exception e = new Exception("smoke");
    verify(e);
  }

  @Test
  public void nested() {
    Exception w = null;
    try {
      someMethod();
    } catch (Exception e) {
      w = new Exception("wrapping", e);
    }
    verify(w);
  }

  @Test
  public void multiNested() {
    Exception w = null;
    try {
      someOtherMethod();
    } catch (Exception e) {
      w = new Exception("wrapping", e);
    }
    verify(w);
  }
  
  void someMethod() throws Exception {
    throw new Exception("someMethod");
  }

  void someOtherMethod() throws Exception {
    try {
      someMethod();
    } catch (Exception e) {
      throw new Exception("someOtherMethod", e);
    }
  }
}
