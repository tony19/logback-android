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

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.util.CachingDateFormatter;
import ch.qos.logback.core.util.DatePatternToRegexUtil;

/**
 * Returns a date formatted by SimpleDateFormatter.
 *
 * @author Ceki G&uuml;c&uuml;
 */
public class DateTokenConverter<E> extends DynamicConverter<E> implements MonoTypedConverter {

  /**
   * The conversion word/character with which this converter is registered.
   */
  public final static String CONVERTER_KEY = "d";
  public final static String AUXILIARY_TOKEN = "AUX";
  public static final String DEFAULT_DATE_PATTERN = CoreConstants.DAILY_DATE_PATTERN;

  private String datePattern;
  private TimeZone timeZone;
  private CachingDateFormatter cdf;
  // is this token converter primary or auxiliary? Only the primary converter
  // determines the rolling period
  private boolean primary = true;
  public void start() {
    this.datePattern = getFirstOption();
    if (this.datePattern == null) {
      this.datePattern = DEFAULT_DATE_PATTERN;
    }

    final List<String> optionList = getOptionList();
    if (optionList != null) {
      for (int optionIndex = 1; optionIndex < optionList.size(); optionIndex++) {
        String option = optionList.get(optionIndex);
        if (AUXILIARY_TOKEN.equalsIgnoreCase(option)) {
          primary = false;
        } else {
          timeZone = TimeZone.getTimeZone(option);
        }
      }
    }

    cdf = new CachingDateFormatter(datePattern);
    if (timeZone != null) {
      cdf.setTimeZone(timeZone);
    }
  }

  public String convert(Date date) {
    return cdf.format(date.getTime());
  }

  public String convert(Object o) {
    if (o == null) {
      throw new IllegalArgumentException("Null argument forbidden");
    }
    if (o instanceof Date) {
      return convert((Date) o);
    }
    throw new IllegalArgumentException("Cannot convert "+o+" of type"+o.getClass().getName());
  }

  /**
   * Return the date pattern.
   * @return the date pattern
   */
  public String getDatePattern() {
    return datePattern;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public boolean isApplicable(Object o) {
    return (o instanceof Date);
  }

  public String toRegex() {
    DatePatternToRegexUtil datePatternToRegexUtil = new DatePatternToRegexUtil(datePattern);
    return datePatternToRegexUtil.toRegex();
  }

  public boolean isPrimary() {
    return primary;
  }
}
