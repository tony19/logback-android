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
package ch.qos.logback.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A synchronized implementation of  SimpleDateFormat which uses caching internally.
 *
 * @author Ceki G&uuml;c&uuml;
 * @since 0.9.29
 */
public class CachingDateFormatter {


  long lastTimestamp = -1;
  String cachedStr = null;
  final SimpleDateFormat sdf;

  public CachingDateFormatter(String pattern) {
    this(pattern, Locale.US);
  }

  public CachingDateFormatter(String pattern, Locale locale) {
    sdf = new SimpleDateFormat(pattern, locale);
  }

  public final String format(long now) {

    // SimpleDateFormat is not thread safe.

    // See also the discussion in http://jira.qos.ch/browse/LBCLASSIC-36
    // DateFormattingThreadedThroughputCalculator and SelectiveDateFormattingRunnable
    // are also note worthy

    // The now == lastTimestamp guard minimizes synchronization
    synchronized (this) {
      if (now != lastTimestamp) {
        lastTimestamp = now;
        cachedStr = sdf.format(new Date(now));
      }
      return cachedStr;
    }
  }

  public void setTimeZone(TimeZone tz) {
    sdf.setTimeZone(tz);
  }
}
