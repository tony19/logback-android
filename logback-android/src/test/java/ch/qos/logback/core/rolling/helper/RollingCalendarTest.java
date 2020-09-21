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

import static ch.qos.logback.core.rolling.helper.RollingCalendar.GMT_TIMEZONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import ch.qos.logback.core.util.EnvUtil;

public class RollingCalendarTest {

  @Test
  public void roundsDateWithMissingMonthDayUnits() throws ParseException {
    final Date REF_DATE = parseDate("yyyy-MM-dd HH:mm:ss.SSS", "2000-12-25 09:30:49.876");
    Calendar cal = getEndOfNextNthPeriod("yyyy-SSS", REF_DATE, -1);

    assertEquals(2000, cal.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
    assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(875, cal.get(Calendar.MILLISECOND));
  }

  @Test
  public void roundsDateWithOnlyDayInYear() throws ParseException {
    final Date REF_DATE = parseDate("yyyy-MM-dd HH:mm:ss.SSS", "2000-12-25 09:30:49.876");
    Calendar cal = getEndOfNextNthPeriod("yyyy-DD", REF_DATE, -1);

    assertEquals(2000, cal.get(Calendar.YEAR));
    assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
    assertEquals(24, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(359, cal.get(Calendar.DAY_OF_YEAR));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
  }

  @Test
  public void roundsDateWithMissingMonthUnits() throws ParseException {
    final Date REF_DATE = parseDate("yyyy-MM-dd HH:mm:ss.SSS", "2000-12-25 09:30:49.876");
    Calendar cal = getEndOfNextNthPeriod("yyyy-dd", REF_DATE, -1);

    assertEquals(2000, cal.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
    assertEquals(24, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
  }

  @Test
  public void roundsDateWithMissingTimeUnits() throws ParseException {
    final Date REF_DATE = parseDate("yyyy-MM-dd HH:mm:ss.SSS", "2000-12-25 09:30:49.876");
    Calendar cal = getEndOfNextNthPeriod("yyyy-MM-dd-ss", REF_DATE, -1);

    assertEquals(2000, cal.get(Calendar.YEAR));
    assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
    assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(48, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
  }

  private Calendar getEndOfNextNthPeriod(String dateFormat, Date date, int n) {
    RollingCalendar rc = new RollingCalendar(dateFormat, GMT_TIMEZONE, Locale.US);
    Date nextDate = rc.getEndOfNextNthPeriod(date, n);
    Calendar cal = Calendar.getInstance(GMT_TIMEZONE, Locale.US);
    cal.setTime(nextDate);
    return cal;
  }

  private Date parseDate(String dateFormat, String dateString) throws ParseException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.US);
    dateFormatter.setTimeZone(GMT_TIMEZONE);
    return dateFormatter.parse(dateString);
  }

  @Test
  public void testMillisecondPeriodicity() {
    // The length of the 'S' pattern letter matters on different platforms,
    // and can render different results on different versions of Android.
    // This test verifies that the periodicity is correct for different
    // pattern lengths.

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd-S");
      assertEquals(PeriodicityType.TOP_OF_MILLISECOND, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd-SS");
      assertEquals(PeriodicityType.TOP_OF_MILLISECOND, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd-SSS");
      assertEquals(PeriodicityType.TOP_OF_MILLISECOND, rc.getPeriodicityType());
    }
  }

  @Test
  public void testPeriodicity() {
    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd_HH_mm_ss");
      assertEquals(PeriodicityType.TOP_OF_SECOND, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd_HH_mm");
      assertEquals(PeriodicityType.TOP_OF_MINUTE, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd_HH");
      assertEquals(PeriodicityType.TOP_OF_HOUR, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd_hh");
      assertEquals(PeriodicityType.TOP_OF_HOUR, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM-dd");
      assertEquals(PeriodicityType.TOP_OF_DAY, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-MM");
      assertEquals(PeriodicityType.TOP_OF_MONTH, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-ww");
      assertEquals(PeriodicityType.TOP_OF_WEEK, rc.getPeriodicityType());
    }

    {
      RollingCalendar rc = new RollingCalendar("yyyy-WW");
      assertEquals(PeriodicityType.TOP_OF_WEEK, rc.getPeriodicityType());
    }
  }

  @Test
  public void testVaryingNumberOfHourlyPeriods() {
    RollingCalendar rc = new RollingCalendar("yyyy-MM-dd_HH");

    long MILLIS_IN_HOUR = 3600 * 1000;

    for (int p = 100; p > -100; p--) {
      long now = 1223325293589L; // Mon Oct 06 22:34:53 CEST 2008
      Date result = rc.getEndOfNextNthPeriod(new Date(now), p);
      long expected = now - (now % (MILLIS_IN_HOUR)) + p * MILLIS_IN_HOUR;
      assertEquals(expected, result.getTime());
    }
  }

  @Test
  public void testVaryingNumberOfDailyPeriods() {
    RollingCalendar rc = new RollingCalendar("yyyy-MM-dd");
    final long MILLIS_IN_DAY = 24 * 3600 * 1000;

    for (int p = 20; p > -100; p--) {
      long now = 1223325293589L; // Mon Oct 06 22:34:53 CEST 2008
      Date nowDate = new Date(now);
      Date result = rc.getEndOfNextNthPeriod(nowDate, p);
      long offset = rc.getTimeZone().getRawOffset() + rc.getTimeZone().getDSTSavings();

      long origin = now - ((now + offset) % (MILLIS_IN_DAY));
      long expected = origin + p * MILLIS_IN_DAY;
      assertEquals("p=" + p, expected, result.getTime());
    }
  }

  @Test
  public void testCollisionFreeness() {
    // hourly
    checkCollisionFreeness("yyyy-MM-dd hh", false);
    checkCollisionFreeness("yyyy-MM-dd hh a", true);

    checkCollisionFreeness("yyyy-MM-dd HH", true);
    checkCollisionFreeness("yyyy-MM-dd kk", true);

    checkCollisionFreeness("yyyy-MM-dd KK", false);
    checkCollisionFreeness("yyyy-MM-dd KK a", true);

    // daily
    checkCollisionFreeness("yyyy-MM-dd", true);
    checkCollisionFreeness("yyyy-dd", false);
    checkCollisionFreeness("dd", false);
    checkCollisionFreeness("MM-dd", false);

    checkCollisionFreeness("yyyy-DDD", true);
    checkCollisionFreeness("DDD", false);

    // 'u' is new to JDK 7
    if (EnvUtil.isJDK7OrHigher()) {
      checkCollisionFreeness("yyyy-MM-dd-uu", true);
      checkCollisionFreeness("yyyy-MM-uu", false);
    }

    // weekly
    checkCollisionFreeness("yyyy-MM-WW", true);
    checkCollisionFreeness("yyyy-WW", false);
    checkCollisionFreeness("yyyy-ww", true);
    checkCollisionFreeness("ww", false);
  }

  private void checkCollisionFreeness(String pattern, boolean expected) {
    RollingCalendar rc = new RollingCalendar(pattern);
    if (expected) {
      assertTrue(rc.isCollisionFree());
    } else {
      assertFalse(rc.isCollisionFree());
    }
  }
}
