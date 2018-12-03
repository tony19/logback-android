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
