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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;

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

  @Test
  public void unparsablePatternIsReportedAsError() {
    FileNamePattern fnp = new FileNamePattern("app-%d{yyyy-MM-dd.log", context);

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("Failed to parse pattern \"app-%d{yyyy-MM-dd.log\".", status.getMessage());
    assertTrue(status.getThrowable() instanceof ScanException);
    // nothing to convert
    assertNull(fnp.headTokenConverter);
    assertEquals("", fnp.convertInt(1));
    assertEquals("app-%d{yyyy-MM-dd.log", fnp.getPattern());
  }

  @Test
  public void equalityAndHashCodeDependOnThePatternOnly() {
    FileNamePattern fnp = new FileNamePattern("app-%i.log", context);
    FileNamePattern samePattern = new FileNamePattern("app-%i.log", new ContextBase());
    FileNamePattern otherPattern = new FileNamePattern("other-%i.log", context);

    assertTrue(fnp.equals(fnp));
    assertTrue(fnp.equals(samePattern));
    assertEquals(fnp.hashCode(), samePattern.hashCode());
    assertEquals(31 + "app-%i.log".hashCode(), fnp.hashCode());
    assertFalse(fnp.equals(otherPattern));
    assertFalse(fnp.equals(null));
    assertFalse(fnp.equals("app-%i.log"));
    assertFalse(fnp.equals(new FileNamePattern("app-%i.log", context) {}));
  }

  @Test
  public void equalityAndHashCodeHandleMissingPattern() {
    FileNamePattern withoutPattern = new FileNamePattern("app-%i.log", context);
    withoutPattern.pattern = null;
    FileNamePattern alsoWithoutPattern = new FileNamePattern("other-%i.log", context);
    alsoWithoutPattern.pattern = null;
    FileNamePattern withPattern = new FileNamePattern("app-%i.log", context);

    assertEquals(31, withoutPattern.hashCode());
    assertTrue(withoutPattern.equals(alsoWithoutPattern));
    assertFalse(withoutPattern.equals(withPattern));
    assertFalse(withPattern.equals(withoutPattern));
  }

  @Test
  public void settingNullPatternKeepsCurrentPattern() {
    FileNamePattern fnp = new FileNamePattern("app-%i.log", context);

    fnp.setPattern(null);

    assertEquals("app-%i.log", fnp.getPattern());
    assertEquals("app-%i.log", fnp.toString());
  }

  @Test
  public void asRegexForFixedDateKeepsAuxiliaryDateVariable() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.clear();
    cal.set(2019, Calendar.NOVEMBER, 4);
    FileNamePattern fnp = new FileNamePattern("logs/%d{yyyy/MM, aux}/app-%d{yyyy-MM-dd, GMT}.log", context);

    String regex = fnp.toRegexForFixedDate(cal.getTime());

    assertEquals("logs/" + FileFinder.regexEscapePath("\\d{4}/\\d{2}") + "/app-2019-11-04.log", regex);
    Pattern p = Pattern.compile(FileFinder.unescapePath(regex));
    // the auxiliary token matches any directory, the primary one only the fixed date
    assertTrue(p.matcher("logs/2018/03/app-2019-11-04.log").matches());
    assertFalse(p.matcher("logs/2019/11/app-2019-11-05.log").matches());
  }

  @Test
  public void asRegexIgnoresConvertersOfOtherTypes() {
    FileNamePattern fnp = new FileNamePattern("unused", context);
    // only literal, integer and date converters contribute to a regex
    Converter<Object> head = new LiteralConverter<Object>("a");
    Converter<Object> other = new Converter<Object>() {
      @Override
      public String convert(Object event) {
        return "X";
      }
    };
    head.setNext(other);
    other.setNext(new LiteralConverter<Object>("b"));
    fnp.headTokenConverter = head;

    assertEquals("ab", fnp.toRegex());
    assertEquals("ab", fnp.toRegexForFixedDate(new Date(0)));
    // whereas the converter does take part in plain conversions
    assertEquals("aXb", fnp.convert(null));
  }
}
