/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
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

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.util.CachingDateFormatter;

public class DateConverter extends ClassicConverter {

  CachingDateFormatter cachingDateFormatter = null;

  public void start() {


    String datePattern = getFirstOption();
    if (datePattern == null) {
      datePattern = CoreConstants.ISO8601_PATTERN;
    }

    if (datePattern.equals(CoreConstants.ISO8601_STR)) {
      datePattern = CoreConstants.ISO8601_PATTERN;
    }

    TimeZone tz = TimeZone.getDefault();
    Locale locale = Locale.ENGLISH;

    List<String> optionList = getOptionList();
    if (optionList != null) {
      // Option[1] = Time zone
      if (optionList.size() > 1) {
        tz = TimeZone.getTimeZone(optionList.get(1));
      }

      // Option[2] = Locale
      if (optionList.size() > 2) {
        locale = parseLocale(optionList.get(2));
      }
    }

    try {
      cachingDateFormatter = new CachingDateFormatter(datePattern, locale);
    } catch (IllegalArgumentException e) {
      addWarn("Could not instantiate SimpleDateFormat with pattern "
          + datePattern, e);
      // default to the ISO8601 format
      cachingDateFormatter = new CachingDateFormatter(CoreConstants.ISO8601_PATTERN, locale);
    }

    cachingDateFormatter.setTimeZone(tz);
  }

  private Locale parseLocale(String input) {
    String[] localeParts = input.split(",");

    Locale locale;
    if (localeParts.length > 1) {
      locale = new Locale(localeParts[0], localeParts[1]);
    } else {
      locale = new Locale(localeParts[0]);
    }
    return locale;
  }

  public String convert(ILoggingEvent le) {
    long timestamp = le.getTimeStamp();
    return cachingDateFormatter.format(timestamp);
  }
}
