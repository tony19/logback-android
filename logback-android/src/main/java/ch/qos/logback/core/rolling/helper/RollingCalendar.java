/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.rolling.helper;

import static ch.qos.logback.core.CoreConstants.MILLIS_IN_ONE_HOUR;
import static ch.qos.logback.core.CoreConstants.MILLIS_IN_ONE_DAY;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * RollingCalendar is a helper class to
 * {@link ch.qos.logback.core.rolling.TimeBasedRollingPolicy } or similar
 * timed-based rolling policies. Given a periodicity type and the current time,
 * it computes the start of the next interval (i.e. the triggering date).
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class RollingCalendar extends GregorianCalendar {

  private static final long serialVersionUID = -5937537740925066161L;

  // The gmtTimeZone is used only in computeCheckPeriod() method.
  static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

  PeriodicityType periodicityType = PeriodicityType.ERRONEOUS;
  String datePattern;

  private static final HashMap<Character, PeriodicityType> PATTERN_LETTER_TO_PERIODICITY = new LinkedHashMap<>();
  static {
    // ordered from smallest to largest time unit
    PATTERN_LETTER_TO_PERIODICITY.put('S', PeriodicityType.TOP_OF_MILLISECOND); // Millisecond
    PATTERN_LETTER_TO_PERIODICITY.put('s', PeriodicityType.TOP_OF_SECOND); // Second in minute
    PATTERN_LETTER_TO_PERIODICITY.put('m', PeriodicityType.TOP_OF_MINUTE); // Minute in hour
    PATTERN_LETTER_TO_PERIODICITY.put('h', PeriodicityType.TOP_OF_HOUR); // Hour in am/pm (1-12)
    PATTERN_LETTER_TO_PERIODICITY.put('K', PeriodicityType.TOP_OF_HOUR); // Hour in am/pm (0-11)
    PATTERN_LETTER_TO_PERIODICITY.put('k', PeriodicityType.TOP_OF_HOUR); // Hour in day (1-24)
    PATTERN_LETTER_TO_PERIODICITY.put('H', PeriodicityType.TOP_OF_HOUR); // Hour in day (0-23)
    PATTERN_LETTER_TO_PERIODICITY.put('a', PeriodicityType.HALF_DAY); // Am/pm marker
    PATTERN_LETTER_TO_PERIODICITY.put('u', PeriodicityType.TOP_OF_DAY); // Day number of week
    PATTERN_LETTER_TO_PERIODICITY.put('E', PeriodicityType.TOP_OF_DAY); // Day name in week
    PATTERN_LETTER_TO_PERIODICITY.put('F', PeriodicityType.TOP_OF_DAY); // Day of week in month
    PATTERN_LETTER_TO_PERIODICITY.put('d', PeriodicityType.TOP_OF_DAY); // Day in month
    PATTERN_LETTER_TO_PERIODICITY.put('D', PeriodicityType.TOP_OF_DAY); // Day in year
    PATTERN_LETTER_TO_PERIODICITY.put('W', PeriodicityType.TOP_OF_WEEK); // Week in month
    PATTERN_LETTER_TO_PERIODICITY.put('w', PeriodicityType.TOP_OF_WEEK); // Week in year
    PATTERN_LETTER_TO_PERIODICITY.put('M', PeriodicityType.TOP_OF_MONTH); // Month in year (context sensitive)
    PATTERN_LETTER_TO_PERIODICITY.put('Y', PeriodicityType.TOP_OF_WEEK); // Week year
  }

  public RollingCalendar(String datePattern) {
    this(datePattern, GMT_TIMEZONE, Locale.US);
  }

  public RollingCalendar(String datePattern, TimeZone tz, Locale locale) {
    super(tz, locale);
    this.datePattern = datePattern;
    this.periodicityType = computePeriodicityType();
  }

  public PeriodicityType getPeriodicityType() {
    return periodicityType;
  }

  public PeriodicityType computePeriodicityType() {
    if (datePattern != null) {
      for (Map.Entry<Character, PeriodicityType> entry : PATTERN_LETTER_TO_PERIODICITY.entrySet()) {
        if (datePattern.indexOf(entry.getKey()) > -1) {
          return entry.getValue();
        }
      }
    }
    // we failed
    return PeriodicityType.ERRONEOUS;
  }

  public boolean isCollisionFree() {
    switch (periodicityType) {
      case TOP_OF_HOUR:
        // isolated hh or KK
        return !collision(12 * MILLIS_IN_ONE_HOUR);

      case TOP_OF_DAY:
        // EE or uu
        if(collision(7 * MILLIS_IN_ONE_DAY))
          return false;
        // isolated dd
        if(collision(31 * MILLIS_IN_ONE_DAY))
          return false;
        // DD
        if(collision(365 * MILLIS_IN_ONE_DAY))
          return false;
        return true;
      case TOP_OF_WEEK:
        // WW
        if(collision(34 * MILLIS_IN_ONE_DAY))
          return false;
        // isolated ww
        if(collision(366 * MILLIS_IN_ONE_DAY))
          return false;
        return true;
      default:
        return true;
    }
  }

  private boolean collision(long delta) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern, Locale.US);
    simpleDateFormat.setTimeZone(GMT_TIMEZONE); // all date formatting done in GMT
    Date epoch0 = new Date(0);
    String r0 = simpleDateFormat.format(epoch0);
    Date epoch12 = new Date(delta);
    String r12 = simpleDateFormat.format(epoch12);

    return r0.equals(r12);
  }

  public void printPeriodicity(ContextAwareBase cab) {
    switch (periodicityType) {
      case TOP_OF_MILLISECOND:
        cab.addInfo("Roll-over every millisecond.");
        break;

      case TOP_OF_SECOND:
        cab.addInfo("Roll-over every second.");
        break;

      case TOP_OF_MINUTE:
        cab.addInfo("Roll-over every minute.");
        break;

      case TOP_OF_HOUR:
        cab.addInfo("Roll-over at the top of every hour.");
        break;

      case HALF_DAY:
        cab.addInfo("Roll-over at midday and midnight.");
        break;

      case TOP_OF_DAY:
        cab.addInfo("Roll-over at midnight.");
        break;

      case TOP_OF_WEEK:
        cab.addInfo("Rollover at the start of week.");
        break;

      case TOP_OF_MONTH:
        cab.addInfo("Rollover at start of every month.");
        break;

      default:
        cab.addInfo("Unknown periodicity.");
    }
  }

  public long periodBarriersCrossed(long start, long end) {
    if (start > end)
      throw new IllegalArgumentException("Start cannot come before end");

    long startFloored = getStartOfCurrentPeriodWithGMTOffsetCorrection(start, getTimeZone());
    long endFloored = getStartOfCurrentPeriodWithGMTOffsetCorrection(end, getTimeZone());

    long diff = endFloored - startFloored;

    switch (periodicityType) {

      case TOP_OF_MILLISECOND:
        return diff;
      case TOP_OF_SECOND:
        return diff / CoreConstants.MILLIS_IN_ONE_SECOND;
      case TOP_OF_MINUTE:
        return diff / CoreConstants.MILLIS_IN_ONE_MINUTE;
      case TOP_OF_HOUR:
        return diff / CoreConstants.MILLIS_IN_ONE_HOUR;
      case TOP_OF_DAY:
        return diff / CoreConstants.MILLIS_IN_ONE_DAY;
      case TOP_OF_WEEK:
        return diff / CoreConstants.MILLIS_IN_ONE_WEEK;
      case TOP_OF_MONTH:
        return diffInMonths(start, end);
      default:
        throw new IllegalStateException("Unknown periodicity type.");
    }
  }

  public static int diffInMonths(long startTime, long endTime) {
    if (startTime > endTime)
      throw new IllegalArgumentException("startTime cannot be larger than endTime");
    Calendar startCal = Calendar.getInstance();
    startCal.setTimeInMillis(startTime);
    Calendar endCal = Calendar.getInstance();
    endCal.setTimeInMillis(endTime);
    int yearDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
    int monthDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);
    return yearDiff * 12 + monthDiff;
  }

  private Date innerGetEndOfNextNthPeriod(Calendar cal, PeriodicityType periodicityType, Date now, int numPeriods) {
    cal.setTime(now);

    roundDownTime(cal, this.datePattern);

    switch (periodicityType) {
      case TOP_OF_MILLISECOND:
        cal.add(Calendar.MILLISECOND, numPeriods);
        break;

      case TOP_OF_SECOND:
        cal.add(Calendar.SECOND, numPeriods);
        break;

      case TOP_OF_MINUTE:
        cal.add(Calendar.MINUTE, numPeriods);
        break;

      case TOP_OF_HOUR:
        cal.add(Calendar.HOUR_OF_DAY, numPeriods);
        break;

      case TOP_OF_DAY:
        cal.add(Calendar.DATE, numPeriods);
        break;

      case TOP_OF_WEEK:
        cal.add(Calendar.WEEK_OF_YEAR, numPeriods);
        break;

      case TOP_OF_MONTH:
        cal.set(Calendar.DATE, 1);
        cal.add(Calendar.MONTH, numPeriods);
        break;

      default:
        throw new IllegalStateException("Unknown periodicity type.");
    }

    return cal.getTime();
  }

  public Date getEndOfNextNthPeriod(Date now, int periods) {
    return innerGetEndOfNextNthPeriod(this, this.periodicityType, now, periods);
  }

  public Date getNextTriggeringDate(Date now) {
    return getEndOfNextNthPeriod(now, 1);
  }

  public long getStartOfCurrentPeriodWithGMTOffsetCorrection(long now, TimeZone timezone) {
    Date toppedDate;

    // there is a bug in Calendar which prevents it from
    // computing the correct DST_OFFSET when the time changes
    {
      Calendar aCal = Calendar.getInstance(timezone);
      aCal.setTimeInMillis(now);
      toppedDate = getEndOfNextNthPeriod(aCal.getTime(), 0);
    }
    Calendar secondCalendar = Calendar.getInstance(timezone);
    secondCalendar.setTimeInMillis(toppedDate.getTime());
    long gmtOffset = secondCalendar.get(Calendar.ZONE_OFFSET) + secondCalendar.get(Calendar.DST_OFFSET);
    return toppedDate.getTime() + gmtOffset;
  }

  private void roundDownTime(Calendar cal, String datePattern) {
    if (datePattern.indexOf('S') == -1) {
      cal.set(Calendar.MILLISECOND, 0);
    }
    if (datePattern.indexOf('s') == -1) {
      cal.set(Calendar.SECOND, 0);
    }
    if (datePattern.indexOf('m') == -1) {
      cal.set(Calendar.MINUTE, 0);
    }
    final Pattern hourOfDayPattern = Pattern.compile("[hKkH]");
    if (!hourOfDayPattern.matcher(datePattern).find()) {
      cal.set(Calendar.HOUR_OF_DAY, 0);
    }
    final Pattern dayOfMonthPattern = Pattern.compile("[uEFdD]");
    if (!dayOfMonthPattern.matcher(datePattern).find()) {
      cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
      cal.set(Calendar.DAY_OF_MONTH, 1);
    }
    if (datePattern.indexOf('M') == -1) {
      cal.set(Calendar.MONTH, Calendar.JANUARY);
    }
  }
}
