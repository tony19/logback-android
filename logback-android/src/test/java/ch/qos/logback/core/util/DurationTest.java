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


public class DurationTest  {

  static long HOURS_CO = 60*60;
  static long DAYS_CO = 24*60*60;
  

  @Test
  public void test() {
    {
      Duration d = Duration.valueOf("12");
      assertEquals(12, d.getMilliseconds());
    }

    {
      Duration d = Duration.valueOf("159 milli");
      assertEquals(159, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("15 millis");
      assertEquals(15, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("8 milliseconds");
      assertEquals(8, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("10.7 millisecond");
      assertEquals(10, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("10 SECOnds");
      assertEquals(10 * 1000, d.getMilliseconds());
    }

    {
      Duration d = Duration.valueOf("12seconde");
      assertEquals(12 * 1000, d.getMilliseconds());
    }

    {
      Duration d = Duration.valueOf("14 SECONDES");
      assertEquals(14 * 1000, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("12second");
      assertEquals(12 * 1000, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("10.7 seconds");
      assertEquals(10700, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("1 minute");
      assertEquals(1000*60, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("2.2 minutes");
      assertEquals(2200*60, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("1 hour");
      assertEquals(1000*HOURS_CO, d.getMilliseconds());
    }
    
    {
      Duration d = Duration.valueOf("4.2 hours");
      assertEquals(4200*HOURS_CO, d.getMilliseconds());
    }

    {
      Duration d = Duration.valueOf("5 days");
      assertEquals(5000*DAYS_CO, d.getMilliseconds());
    }
  }

  @Test
  public void valueOfRejectsUnparsableInput() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> Duration.valueOf("5 fortnights"));
    assertEquals("String value [5 fortnights] is not in the expected format.", e.getMessage());
  }

  @Test
  public void valueOfRejectsUnitAdmittedByThePatternButNotHandled() throws Exception {
    // valueOf() handles every unit that its pattern admits; initialize a copy
    // of Duration whose pattern also admits another unit
    Pattern wider = Pattern.compile("([0-9]+)()\\s*(fortnight)s?", Pattern.CASE_INSENSITIVE);
    Class<?> duration;
    try (MockedStatic<Pattern> patterns = mockStatic(Pattern.class, CALLS_REAL_METHODS)) {
      patterns.when(() -> Pattern.compile(anyString(), eq(Pattern.CASE_INSENSITIVE))).thenReturn(wider);
      duration = FreshCopyClassLoader.initializeFreshCopy(Duration.class);
    }
    Method valueOf = duration.getMethod("valueOf", String.class);

    InvocationTargetException e = assertThrows(InvocationTargetException.class,
        () -> valueOf.invoke(null, "2 fortnights"));

    assertTrue(e.getCause() instanceof IllegalStateException);
    assertEquals("Unexpected fortnight", e.getCause().getMessage());
  }

  @Test
  public void unboundedIsLongMaxValue() {
    assertEquals(Long.MAX_VALUE, Duration.buildUnbounded().getMilliseconds());
  }

  @Test
  public void toStringUsesTheLargestFittingUnit() {
    assertEquals("999 milliseconds", new Duration(999).toString());
    assertEquals("1 seconds", new Duration(1000).toString());
    assertEquals("59 seconds", new Duration(59999).toString());
    assertEquals("1 minutes", new Duration(60 * 1000).toString());
    assertEquals("59 minutes", new Duration(HOURS_CO * 1000 - 1).toString());
    assertEquals("1 hours", new Duration(HOURS_CO * 1000).toString());
    assertEquals("48 hours", new Duration(2 * DAYS_CO * 1000).toString());
  }
}
