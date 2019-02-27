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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DateParser implements FilenameParser<Date> {
  private final SimpleDateFormat dateFormatter;
  private final Pattern pathPattern;

  DateParser(FileNamePattern fileNamePattern) {
    this.dateFormatter = getDateFormatter(fileNamePattern);
    String pathRegexString = fileNamePattern.toRegex(true, false);
    this.pathPattern = Pattern.compile(pathRegexString);
  }

  Date parseDate(String dateString) throws ParseException {
    return this.dateFormatter.parse(dateString);
  }

  public Date parseFilename(String filename) {
    Date date = null;

    try {
      date = parseDate(findToken(filename));
    } catch (ParseException e) {
      // ignore
    }

    return date;
  }

  private String findToken(String input) {
    Matcher m = this.pathPattern.matcher(input);
    return (m.find() && m.groupCount() >= 1) ? m.group(1) : "";
  }

  private SimpleDateFormat getDateFormatter(FileNamePattern fileNamePattern) {
    final DateTokenConverter<Object> dateStringConverter = fileNamePattern.getPrimaryDateTokenConverter();
    final String datePattern = dateStringConverter != null ? dateStringConverter.getDatePattern() : DateTokenConverter.DEFAULT_DATE_PATTERN;
    final SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern, Locale.US);
    TimeZone timeZone = dateStringConverter != null ? dateStringConverter.getTimeZone() : TimeZone.getDefault();
    if (timeZone != null) {
      dateFormatter.setTimeZone(timeZone);
    }
    return dateFormatter;
  }
}
