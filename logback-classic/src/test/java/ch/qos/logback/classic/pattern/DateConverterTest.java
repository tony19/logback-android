/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 2015, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.pattern;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import ch.qos.logback.core.CoreConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.core.pattern.FormattingConverter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link DateConverter} class
 */
public class DateConverterTest {

  private static final String DATETIME_PATTERN = "MMM-dd HH:mm:ss.SSS";
  private static final String ENGLISH_TIME_UTC = "Sep-03 17:20:55.123";
  private static final String FRENCH_TIME_UTC = "sept.-03 17:20:55.123";
  private static final String CHINESE_TIME_UTC = "九月-03 17:20:55.123";

  private static LoggerContext _context;
  private static Logger _logger;
  private static Date _date;
  private static long _timestamp;
  private static String _isoDateString;

  @BeforeClass
  public static void beforeClass() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN, Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    _date = sdf.parse(ENGLISH_TIME_UTC);
    _context = new LoggerContext();
    _logger = _context.getLogger(DateConverterTest.class);
    _timestamp = _date.getTime();
    _isoDateString = formatDateWithPattern(_date, CoreConstants.ISO8601_PATTERN, TimeZone.getDefault().getID());
  }

  @Test
  public void convertsDateAsIso8601WhenNull() {
    assertEquals(_isoDateString, convert(_timestamp, new String[]{null}));
  }

  @Test
  public void convertsDateAsIso8601WhenSpecifiedByIsoName() {
    assertEquals(_isoDateString, convert(_timestamp, CoreConstants.ISO8601_STR));
  }

  @Test
  public void convertsDateAsIso8601WhenInvalidPatternSpecified() {
    assertEquals(_isoDateString, convert(_timestamp, "foo"));
  }

  @Test
  public void convertsDateWithEnglishLocaleByDefault() {
    Locale origLocale = Locale.getDefault();
    Locale.setDefault(Locale.FRANCE);
    assertEquals(ENGLISH_TIME_UTC, convert(_timestamp, DATETIME_PATTERN, "UTC"));
    Locale.setDefault(origLocale);
  }

  @Test
  public void convertsDateWithSpecifiedLocaleLang() {
    assertEquals(FRENCH_TIME_UTC, convert(_timestamp, DATETIME_PATTERN, "UTC", "fr"));
  }

  @Test
  public void convertsDateWithSpecifiedLocaleLangAndCountry() {
    assertEquals(CHINESE_TIME_UTC, convert(_timestamp, DATETIME_PATTERN, "UTC", "zh,CN"));
  }

  @Test
  public void convertsDateWithCurrentTimeZoneByDefault() {
    assertEquals(formatDate(TimeZone.getDefault().getID()), convert(_timestamp, DATETIME_PATTERN));
  }

  @Test
  public void convertsDateWithUtcWhenTimeZoneBlank() {
    assertEquals(formatDate("UTC"), convert(_timestamp, DATETIME_PATTERN, ""));
  }

  @Test
  public void convertsDateWithUtcWhenTimeZoneUnknown() {
    assertEquals(formatDate("UTC"), convert(_timestamp, DATETIME_PATTERN, "FakeTimeZone"));
  }

  @Test
  public void convertsDateInSpecifiedTimeZoneAsGmtOffset() {
    assertEquals(formatDate("GMT-8"), convert(_timestamp, DATETIME_PATTERN, "GMT-8"));
  }

  @Test
  public void convertsDateInSpecifiedTimeZoneAsRawOffset() {
    assertEquals(formatDate("-0800"), convert(_timestamp, DATETIME_PATTERN, "-0800"));
  }

  @Test
  public void convertsDateInSpecifiedTimeZoneAsTzid() {
    assertEquals(formatDate("CST"), convert(_timestamp, DATETIME_PATTERN, "CST"));
  }

  /**
   * Gets a string representation of the test date formatted with the prespecified test pattern
   * @param timeZone timezone ID
   * @return the formatted date/time string
   */
  private static String formatDate(String timeZone) {
    return formatDateWithPattern(_date, DATETIME_PATTERN, timeZone);
  }

  /**
   * Gets a string representaton of a date formatted with the specified date/time pattern
   * @param date date to format
   * @param pattern desired date/time pattern
   * @param timeZone timezone ID
   * @return the formatted date/time string
   */
  private static String formatDateWithPattern(Date date, String pattern, String timeZone) {
    SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone(timeZone));

    if (!timeZone.equalsIgnoreCase("UTC")) {
      // TimeZone.getTimeZone() returns UTC for unknown time zones, so make sure
      // that we only get UTC when it's explicitly requested.
      assertThat("unexpected UTC (time zone not found for: \"" + timeZone + "\")",
          sdf.getTimeZone(), is(not(TimeZone.getTimeZone("UTC"))));
    }

    return sdf.format(date);
  }

  /**
   * Generates a LoggingEvent
   * @param timestamp desired _timestamp (in ms) of event
   * @return the newly created LoggingEvent
   */
  private LoggingEvent makeLoggingEvent(long timestamp) {
    LoggingEvent event = new LoggingEvent(FormattingConverter.class.getName(),
        _logger,
        Level.INFO,
        "Some message",
        null,
        null);
    event.setTimeStamp(timestamp);
    return event;
  }

  /**
   * Gets a DateConverter, configured with options and using the default context
   * @param options the conversion pattern's options
   * @return the newly created DateConverter
   */
  private DateConverter getDateConverter(String... options) {
    DateConverter converter = new DateConverter();
    converter.setContext(_context);
    converter.setOptionList(Arrays.asList(options));
    return converter;
  }

  /**
   * Gets the output of a DateConverter, configured with the given settings
   * @param timestamp _timestamp to display
   * @param options the conversion pattern's options
   * @return the DateConverter's output
   */
  private String convert(long timestamp, String... options) {
    DateConverter converter = getDateConverter(options);
    converter.start();
    return converter.convert(makeLoggingEvent(timestamp));
  }
}