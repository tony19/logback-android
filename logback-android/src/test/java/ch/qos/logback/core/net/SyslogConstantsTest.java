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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link SyslogConstants}.
 */
public class SyslogConstantsTest {

  @Test
  public void isInstantiable() {
    // a public class with the implicit public constructor
    assertNotNull(new SyslogConstants());
  }

  @Test
  public void usesTheStandardSyslogPort() {
    assertEquals(514, SyslogConstants.SYSLOG_PORT);
  }

  @Test
  public void severitiesAreTheRfc3164Codes() {
    assertEquals(0, SyslogConstants.EMERGENCY_SEVERITY);
    assertEquals(1, SyslogConstants.ALERT_SEVERITY);
    assertEquals(2, SyslogConstants.CRITICAL_SEVERITY);
    assertEquals(3, SyslogConstants.ERROR_SEVERITY);
    assertEquals(4, SyslogConstants.WARNING_SEVERITY);
    assertEquals(5, SyslogConstants.NOTICE_SEVERITY);
    assertEquals(6, SyslogConstants.INFO_SEVERITY);
    assertEquals(7, SyslogConstants.DEBUG_SEVERITY);
  }

  @Test
  public void facilitiesAreTheRfc3164CodesTimesEight() {
    // the facility part of PRI is precomputed as (facility code * 8)
    int[] facilities = {
        SyslogConstants.LOG_KERN, SyslogConstants.LOG_USER, SyslogConstants.LOG_MAIL,
        SyslogConstants.LOG_DAEMON, SyslogConstants.LOG_AUTH, SyslogConstants.LOG_SYSLOG,
        SyslogConstants.LOG_LPR, SyslogConstants.LOG_NEWS, SyslogConstants.LOG_UUCP,
        SyslogConstants.LOG_CRON, SyslogConstants.LOG_AUTHPRIV, SyslogConstants.LOG_FTP,
        SyslogConstants.LOG_NTP, SyslogConstants.LOG_AUDIT, SyslogConstants.LOG_ALERT,
        SyslogConstants.LOG_CLOCK, SyslogConstants.LOG_LOCAL0, SyslogConstants.LOG_LOCAL1,
        SyslogConstants.LOG_LOCAL2, SyslogConstants.LOG_LOCAL3, SyslogConstants.LOG_LOCAL4,
        SyslogConstants.LOG_LOCAL5, SyslogConstants.LOG_LOCAL6, SyslogConstants.LOG_LOCAL7,
    };
    for (int code = 0; code < facilities.length; code++) {
      assertEquals("facility code " + code, code * 8, facilities[code]);
    }
  }
}
