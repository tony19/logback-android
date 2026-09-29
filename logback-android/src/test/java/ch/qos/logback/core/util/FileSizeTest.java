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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.junit.Test;
import org.mockito.MockedStatic;


public class FileSizeTest{

  static long KB_CO = 1024;
  static long MB_CO = 1024*1024;
  static long GB_CO = 1024*MB_CO;
  

  @Test
  public void testValueOf() {
    {
      FileSize fs = FileSize.valueOf("8");
      assertEquals(8, fs.getSize());
    }
    
    {
      FileSize fs = FileSize.valueOf("8 kbs");
      assertEquals(8*KB_CO, fs.getSize());
    }
  
    {
      FileSize fs = FileSize.valueOf("8 kb");
      assertEquals(8*KB_CO, fs.getSize());
    }
    
    {
      FileSize fs = FileSize.valueOf("12 mb");
      assertEquals(12*MB_CO, fs.getSize());
    }

    {
      FileSize fs = FileSize.valueOf("5 GBs");
      assertEquals(5*GB_CO, fs.getSize());
    }

  }

  @Test
  public void valueOfRejectsUnparsableInput() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FileSize.valueOf("8 tb"));
    assertEquals("String value [8 tb] is not in the expected format.", e.getMessage());
  }

  @Test
  public void valueOfRejectsUnitAdmittedByThePatternButNotHandled() throws Exception {
    // valueOf() handles every unit that its pattern admits; initialize a copy
    // of FileSize whose pattern also admits another unit
    Pattern wider = Pattern.compile("([0-9]+)\\s*(tb)s?", Pattern.CASE_INSENSITIVE);
    Class<?> fileSize;
    try (MockedStatic<Pattern> patterns = mockStatic(Pattern.class, CALLS_REAL_METHODS)) {
      patterns.when(() -> Pattern.compile(anyString(), eq(Pattern.CASE_INSENSITIVE))).thenReturn(wider);
      fileSize = FreshCopyClassLoader.initializeFreshCopy(FileSize.class);
    }
    Method valueOf = fileSize.getMethod("valueOf", String.class);

    InvocationTargetException e = assertThrows(InvocationTargetException.class,
        () -> valueOf.invoke(null, "3 tb"));

    assertTrue(e.getCause() instanceof IllegalStateException);
    assertEquals("Unexpected tb", e.getCause().getMessage());
  }

  @Test
  public void testToString() {
    {
      FileSize fs = new FileSize(8);
      assertEquals("8 Bytes", fs.toString());
    }
    {
      FileSize fs = new FileSize(8 * 1024 + 3);
      assertEquals("8 KB", fs.toString());
    }

    {
      FileSize fs = new FileSize(8 * 1024 * 1024 + 3 * 1024);
      assertEquals("8 MB", fs.toString());
    }

    {
      FileSize fs = new FileSize(8*1024*1024*1024L);
      assertEquals("8 GB", fs.toString());
    }
  }

}
