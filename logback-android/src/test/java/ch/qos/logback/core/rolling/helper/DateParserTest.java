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
import static org.junit.Assert.assertNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

public class DateParserTest {

  // a fixed default time zone that differs from GMT, so that parsing in the
  // default zone and parsing in GMT give different dates
  private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("GMT+05:30");

  private final Context context = new ContextBase();
  private TimeZone originalDefaultTimeZone;

  @Before
  public void setUp() {
    originalDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(DEFAULT_TIME_ZONE);
  }

  @After
  public void tearDown() {
    TimeZone.setDefault(originalDefaultTimeZone);
  }

  @Test
  public void parsesPrimaryDateOfMatchingFilenameInItsTimeZone() throws ParseException {
    DateParser parser = new DateParser(new FileNamePattern("/logs/app-%d{yyyy-MM-dd, GMT}.log", context));

    assertEquals(parse("yyyy-MM-dd", TimeZone.getTimeZone("GMT"), "2019-11-04"),
        parser.parseFilename("/logs/app-2019-11-04.log"));
  }

  @Test
  public void parsesPrimaryDateWithoutTimeZoneInDefaultTimeZone() throws ParseException {
    DateParser parser = new DateParser(new FileNamePattern("/logs/app-%d{yyyy-MM-dd}.log", context));

    assertEquals(parse("yyyy-MM-dd", DEFAULT_TIME_ZONE, "2019-11-04"),
        parser.parseFilename("/logs/app-2019-11-04.log"));
  }

  @Test
  public void filenameNotMatchingThePatternHasNoDate() {
    DateParser parser = new DateParser(new FileNamePattern("/logs/app-%d{yyyy-MM-dd, GMT}.log", context));

    assertNull(parser.parseFilename("/logs/other.txt"));
  }

  @Test
  public void patternWithoutPrimaryDateYieldsNoDate() {
    DateParser parser = new DateParser(new FileNamePattern("/logs/app-%i.log", context));

    // the filename matches, but there is no date token to capture
    assertNull(parser.parseFilename("/logs/app-1.log"));
  }

  @Test
  public void patternWithoutPrimaryDateParsesWithDefaultDatePatternAndTimeZone() throws ParseException {
    DateParser parser = new DateParser(new FileNamePattern("/logs/app-%d{yyyy/MM, aux}-%i.log", context));

    assertEquals(parse(DateTokenConverter.DEFAULT_DATE_PATTERN, DEFAULT_TIME_ZONE, "2019-11-04"),
        parser.parseDate("2019-11-04"));
  }

  private static Date parse(String datePattern, TimeZone timeZone, String date) throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat(datePattern, Locale.US);
    format.setTimeZone(timeZone);
    return format.parse(date);
  }
}
