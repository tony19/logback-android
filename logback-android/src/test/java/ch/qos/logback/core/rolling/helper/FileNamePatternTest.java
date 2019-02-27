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
package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

/**
 * @author Ceki
 * 
 */
public class FileNamePatternTest {

  Context context = new ContextBase();

  @Test
  public void testSmoke() {
    FileNamePattern pp = new FileNamePattern("t", context);
    assertEquals("t", pp.convertInt(3));

    pp = new FileNamePattern("foo", context);
    assertEquals("foo", pp.convertInt(3));

    pp = new FileNamePattern("%i foo", context);

    assertEquals("3 foo", pp.convertInt(3));

    pp = new FileNamePattern("foo%i.xixo", context);
    assertEquals("foo3.xixo", pp.convertInt(3));

    pp = new FileNamePattern("foo%i.log", context);
    assertEquals("foo3.log", pp.convertInt(3));

    pp = new FileNamePattern("foo.%i.log", context);
    assertEquals("foo.3.log", pp.convertInt(3));

    pp = new FileNamePattern("foo.%3i.log", context);
    assertEquals("foo.003.log", pp.convertInt(3));

    pp = new FileNamePattern("foo.%1i.log", context);
    assertEquals("foo.43.log", pp.convertInt(43));

    //pp = new FileNamePattern("%i.foo\\%", context);
    //assertEquals("3.foo%", pp.convertInt(3));

    //pp = new FileNamePattern("\\%foo", context);
    //assertEquals("%foo", pp.convertInt(3));
  }

  @Test
  // test ways for dealing with flowing i converter, as in "foo%ix"
  public void flowingI() {
    {
      FileNamePattern pp = new FileNamePattern("foo%i{}bar%i", context);
      assertEquals("foo3bar3", pp.convertInt(3));
    }
    {
      FileNamePattern pp = new FileNamePattern("foo%i{}bar%i", context);
      assertEquals("foo3bar3", pp.convertInt(3));
    }
  }

  @Test
  public void date() {
    Calendar cal = Calendar.getInstance();
    cal.set(2003, 4, 20, 17, 55);

    FileNamePattern pp = new FileNamePattern("foo%d{yyyy.MM.dd}", context);

    assertEquals("foo2003.05.20", pp.convert(cal.getTime()));

    pp = new FileNamePattern("foo%d{yyyy.MM.dd HH:mm}", context);
    assertEquals("foo2003.05.20 17:55", pp.convert(cal.getTime()));

    pp = new FileNamePattern("%d{yyyy.MM.dd HH:mm} foo", context);
    assertEquals("2003.05.20 17:55 foo", pp.convert(cal.getTime()));

  }

  @Test
  public void dateWithTimeZone() {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    Calendar cal = Calendar.getInstance(utc);
    cal.set(2003, 4, 20, 10, 55);

    FileNamePattern fnp = new FileNamePattern("foo%d{yyyy-MM-dd'T'HH:mm, Australia/Perth}", context);
    // Perth is 8 hours ahead of UTC
    assertEquals("foo2003-05-20T18:55", fnp.convert(cal.getTime()));
  }

  @Test
  public void auxAndTimeZoneShouldNotConflict() {
    TimeZone utc = TimeZone.getTimeZone("UTC");
    Calendar cal = Calendar.getInstance(utc);
    cal.set(2003, 4, 20, 10, 55);

    {
      FileNamePattern fnp = new FileNamePattern("foo%d{yyyy-MM-dd'T'HH:mm, aux, Australia/Perth}", context);
      // Perth is 8 hours ahead of UTC
      assertEquals("foo2003-05-20T18:55", fnp.convert(cal.getTime()));
      assertNull(fnp.getPrimaryDateTokenConverter());
    }

    {
      FileNamePattern fnp = new FileNamePattern("folder/%d{yyyy/MM, aux, Australia/Perth}/test.%d{yyyy-MM-dd'T'HHmm, Australia/Perth}.log", context);
      assertEquals("folder/2003/05/test.2003-05-20T1855.log", fnp.convert(cal.getTime()));
      assertNotNull(fnp.getPrimaryDateTokenConverter());
    }
  } 

  @Test
  public void withBackslash() {
    FileNamePattern pp = new FileNamePattern("c:\\foo\\bar.%i", context);
    assertEquals("c:/foo/bar.3", pp.convertInt(3));
  }

  @Test
  public void objectListConverter() {
    Calendar cal = Calendar.getInstance();
    cal.set(2003, 4, 20, 17, 55);
    FileNamePattern fnp = new FileNamePattern("foo-%d{yyyy.MM.dd}-%i.txt",
        context);
    assertEquals("foo-2003.05.20-79.txt", fnp.convertMultipleArguments(cal
        .getTime(), 79));
  }

  @Test
  public void asRegexByDate() {

    Calendar cal = Calendar.getInstance();
    cal.set(2003, 4, 20, 17, 55);

    {
      FileNamePattern fnp = new FileNamePattern("foo-%d{yyyy.MM.dd}-%i.txt",
          context);
      String regex = fnp.toRegexForFixedDate(cal.getTime());
      assertEquals("foo-2003.05.20-" + FileFinder.regexEscapePath("(\\d+)") + ".txt", regex);
    }
    {
      FileNamePattern fnp = new FileNamePattern("\\toto\\foo-%d{yyyy\\MM\\dd}-%i.txt",
          context);
      String regex = fnp.toRegexForFixedDate(cal.getTime());
      assertEquals("/toto/foo-2003/05/20-" + FileFinder.regexEscapePath("(\\d+)") + ".txt", regex);
    }
  }

  @Test
  public void asRegex() {
    {
      FileNamePattern fnp = new FileNamePattern("foo-%d{yyyy.MM.dd}-%i.txt",
          context);
      String regex = fnp.toRegex();
      assertEquals("foo-" + FileFinder.regexEscapePath("\\d{4}\\.\\d{2}\\.\\d{2}") + "-" + FileFinder.regexEscapePath("\\d+") + ".txt", regex);
    }
    {
      FileNamePattern fnp = new FileNamePattern("foo-%d{yyyy.MM.dd'T'}-%i.txt",
          context);
      String regex = fnp.toRegex();
      assertEquals("foo-" + FileFinder.regexEscapePath("\\d{4}\\.\\d{2}\\.\\d{2}T") + "-" + FileFinder.regexEscapePath("\\d+") + ".txt", regex);
    }
  }

  @Test
  public void convertMultipleDates() {
    Calendar cal = Calendar.getInstance();
    cal.set(2003, 4, 20, 17, 55);
    FileNamePattern fnp = new FileNamePattern("foo-%d{yyyy.MM, aux}/%d{yyyy.MM.dd}.txt", context);
    assertEquals("foo-2003.05/2003.05.20.txt", fnp.convert(cal.getTime()));
  }

  @Test
  public void nullTimeZoneByDefault() {
    FileNamePattern fnp = new FileNamePattern("%d{hh}", context);
    assertNull(fnp.getPrimaryDateTokenConverter().getTimeZone());
  }

  @Test
  public void settingTimeZoneOptionHasAnEffect() {
    TimeZone tz = TimeZone.getTimeZone("Australia/Perth");
    FileNamePattern fnp = new FileNamePattern("%d{hh, " + tz.getID() + "}", context);
    assertEquals(tz, fnp.getPrimaryDateTokenConverter().getTimeZone());
  }
}
