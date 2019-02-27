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
package ch.qos.logback.classic.pattern;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogAppenderBase;

public class SyslogStartConverter extends ClassicConverter {

  long lastTimestamp = -1;
  String timesmapStr = null;
  SimpleDateFormat simpleMonthFormat;
  SimpleDateFormat simpleTimeFormat;
  private final Calendar calendar  = Calendar.getInstance(Locale.US);

  final String localHostName = "localhost";
  int facility;

  public void start() {
    int errorCount = 0;
    
    String facilityStr = getFirstOption();
    if (facilityStr == null) {
      addError("was expecting a facility string as an option");
      return;
    }

    facility = SyslogAppenderBase.facilityStringToint(facilityStr);

    try {
      // hours should be in 0-23, see also http://jira.qos.ch/browse/LBCLASSIC-48
      simpleMonthFormat = new SimpleDateFormat("MMM", Locale.US);
      simpleTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
      simpleTimeFormat.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
      simpleMonthFormat.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
    } catch (IllegalArgumentException e) {
      addError("Could not instantiate SimpleDateFormat", e);
      errorCount++;
    }

    if(errorCount == 0) {
      super.start();
    }
  }

  public String convert(ILoggingEvent event) {
    StringBuilder sb = new StringBuilder();

    int pri = facility + LevelToSyslogSeverity.convert(event);
  
    sb.append("<");
    sb.append(pri);
    sb.append(">");
    sb.append(computeTimeStampString(event.getTimeStamp()));
    sb.append(' ');
    sb.append(localHostName);
    sb.append(' ');

    return sb.toString();
  }

  private String computeTimeStampString(long now) {
    synchronized (this) {
      // Since the formatted output is only precise to the second, we can use the same cached string if the current
      // second is the same (stripping off the milliseconds).
      if ((now / 1000) != lastTimestamp) {
        lastTimestamp = now / 1000;
        Date nowDate = new Date(now);
        calendar.setTime(nowDate);
        timesmapStr = String.format(Locale.US, "%s %2d %s", simpleMonthFormat.format(nowDate),
            calendar.get(Calendar.DAY_OF_MONTH), simpleTimeFormat.format(nowDate));
      }
      return timesmapStr;
    }
  }  
}
