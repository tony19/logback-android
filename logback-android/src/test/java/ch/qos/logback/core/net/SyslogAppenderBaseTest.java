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
package ch.qos.logback.core.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SyslogAppenderBaseTest
{
  @Test
  public void testFacilityStringToint() throws InterruptedException
  {
    assertEquals(SyslogConstants.LOG_KERN, SyslogAppenderBase.facilityStringToint("KERN"));
    assertEquals(SyslogConstants.LOG_USER, SyslogAppenderBase.facilityStringToint("USER"));
    assertEquals(SyslogConstants.LOG_MAIL, SyslogAppenderBase.facilityStringToint("MAIL"));
    assertEquals(SyslogConstants.LOG_DAEMON, SyslogAppenderBase.facilityStringToint("DAEMON"));
    assertEquals(SyslogConstants.LOG_AUTH, SyslogAppenderBase.facilityStringToint("AUTH"));
    assertEquals(SyslogConstants.LOG_SYSLOG, SyslogAppenderBase.facilityStringToint("SYSLOG"));
    assertEquals(SyslogConstants.LOG_LPR, SyslogAppenderBase.facilityStringToint("LPR"));
    assertEquals(SyslogConstants.LOG_NEWS, SyslogAppenderBase.facilityStringToint("NEWS"));
    assertEquals(SyslogConstants.LOG_UUCP, SyslogAppenderBase.facilityStringToint("UUCP"));
    assertEquals(SyslogConstants.LOG_CRON, SyslogAppenderBase.facilityStringToint("CRON"));
    assertEquals(SyslogConstants.LOG_AUTHPRIV, SyslogAppenderBase.facilityStringToint("AUTHPRIV"));
    assertEquals(SyslogConstants.LOG_FTP, SyslogAppenderBase.facilityStringToint("FTP"));
    assertEquals(SyslogConstants.LOG_NTP, SyslogAppenderBase.facilityStringToint("NTP"));
    assertEquals(SyslogConstants.LOG_AUDIT, SyslogAppenderBase.facilityStringToint("AUDIT"));
    assertEquals(SyslogConstants.LOG_ALERT, SyslogAppenderBase.facilityStringToint("ALERT"));
    assertEquals(SyslogConstants.LOG_CLOCK, SyslogAppenderBase.facilityStringToint("CLOCK"));
    assertEquals(SyslogConstants.LOG_LOCAL0, SyslogAppenderBase.facilityStringToint("LOCAL0"));
    assertEquals(SyslogConstants.LOG_LOCAL1, SyslogAppenderBase.facilityStringToint("LOCAL1"));
    assertEquals(SyslogConstants.LOG_LOCAL2, SyslogAppenderBase.facilityStringToint("LOCAL2"));
    assertEquals(SyslogConstants.LOG_LOCAL3, SyslogAppenderBase.facilityStringToint("LOCAL3"));
    assertEquals(SyslogConstants.LOG_LOCAL4, SyslogAppenderBase.facilityStringToint("LOCAL4"));
    assertEquals(SyslogConstants.LOG_LOCAL5, SyslogAppenderBase.facilityStringToint("LOCAL5"));
    assertEquals(SyslogConstants.LOG_LOCAL6, SyslogAppenderBase.facilityStringToint("LOCAL6"));
    assertEquals(SyslogConstants.LOG_LOCAL7, SyslogAppenderBase.facilityStringToint("LOCAL7"));
  }
}
