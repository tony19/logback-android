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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.status.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedConstruction;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;

public class SyslogStartConverterTest {

  private LoggerContext lc;
  private SyslogStartConverter converter;
  private final String HOSTNAME = "localhost";
  private final Calendar calendar = Calendar.getInstance(Locale.US);

  @Before
  public void setUp() throws Exception {
    lc = new LoggerContext();
    converter = new SyslogStartConverter();
    converter.setOptionList(Arrays.asList("local7"));
    converter.start();
  }

  @After
  public void tearDown() throws Exception {
    lc = null;
    converter.stop();
    converter = null;
  }

  @Test
  public void datesLessThanTen() {
    // RFC 3164, section 4.1.2:
    // If the day of the month is less than 10, then it MUST be represented as
    // a space and then the number.  For example, the 7th day of August would be
    // represented as "Aug  7", with two spaces between the "g" and the "7".
    LoggingEvent le = createLoggingEvent();
    calendar.set(2012, Calendar.AUGUST, 7, 13, 15, 0);
    le.setTimeStamp(calendar.getTimeInMillis());
    assertEquals("<191>Aug  7 13:15:00 " + HOSTNAME + " ", converter.convert(le));
  }

  @Test
  public void datesGreaterThanTen() {
    LoggingEvent le = createLoggingEvent();
    calendar.set(2012, Calendar.OCTOBER, 11, 22, 14, 15);
    le.setTimeStamp(calendar.getTimeInMillis());
    assertEquals("<191>Oct 11 22:14:15 " + HOSTNAME + " ", converter.convert(le));
  }

  @Test
  public void multipleConversions() {
    LoggingEvent le = createLoggingEvent();
    calendar.set(2012, Calendar.OCTOBER, 11, 22, 14, 15);
    le.setTimeStamp(calendar.getTimeInMillis());
    assertEquals("<191>Oct 11 22:14:15 " + HOSTNAME + " ", converter.convert(le));
    assertEquals("<191>Oct 11 22:14:15 " + HOSTNAME + " ", converter.convert(le));

    calendar.set(2012, Calendar.OCTOBER, 11, 22, 14, 16);
    le.setTimeStamp(calendar.getTimeInMillis());
    assertEquals("<191>Oct 11 22:14:16 " + HOSTNAME + " ", converter.convert(le));
  }

  @Test
  public void ignoreDefaultLocale() {
    Locale originalDefaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.TRADITIONAL_CHINESE);

    try {
      converter.start();

      LoggingEvent le = createLoggingEvent();
      calendar.set(2012, Calendar.OCTOBER, 11, 22, 14, 15);
      le.setTimeStamp(calendar.getTimeInMillis());
      String result = converter.convert(le);
      assertEquals("<191>Oct 11 22:14:15 " + HOSTNAME + " ", result);
    } finally {
      Locale.setDefault(originalDefaultLocale);
    }
  }

  @Test
  public void hostnameShouldNotIncludeDomain() throws Exception {
    // RFC 3164, section 4.1.2:
    // The Domain Name MUST NOT be included in the HOSTNAME field.
    String host = HOSTNAME;
    final int firstPeriod = host.indexOf(".");
    if (firstPeriod != -1) {
      host = host.substring(0, firstPeriod);
    }
    LoggingEvent le = createLoggingEvent();
    calendar.set(2012, Calendar.OCTOBER, 11, 22, 14, 15);
    le.setTimeStamp(calendar.getTimeInMillis());
    assertEquals("<191>Oct 11 22:14:15 " + host + " ", converter.convert(le));
  }

  @Test
  public void missingFacilityOptionIsReportedAndConverterStaysStopped() {
    SyslogStartConverter c = new SyslogStartConverter();
    c.setContext(lc);
    c.start();

    assertFalse(c.isStarted());
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("was expecting a facility string as an option", statuses.get(0).getMessage());
  }

  @Test
  public void dateFormatSetupFailureIsReportedAndConverterStaysStopped() {
    IllegalArgumentException failure = new IllegalArgumentException("injected");
    SyslogStartConverter c = new SyslogStartConverter();
    c.setContext(lc);
    c.setOptionList(Arrays.asList("local7"));

    try (MockedConstruction<SimpleDateFormat> ignored = mockConstruction(SimpleDateFormat.class,
        (mock, context) -> doThrow(failure).when(mock).setDateFormatSymbols(any(DateFormatSymbols.class)))) {
      c.start();
    }

    assertFalse(c.isStarted());
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.ERROR, statuses.get(0).getLevel());
    assertEquals("Could not instantiate SimpleDateFormat", statuses.get(0).getMessage());
    assertSame(failure, statuses.get(0).getThrowable());
  }

  private LoggingEvent createLoggingEvent() {
    return new LoggingEvent(this.getClass().getName(), lc
        .getLogger(Logger.ROOT_LOGGER_NAME), Level.DEBUG, "test message", null,
        null);
  }
}
