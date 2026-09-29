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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SMTPAppender;

public class SMTPAppenderBaseTest {

  private static final String CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";

  private final SMTPAppender appender = new SMTPAppender();

  @After
  public void tearDown() {
    System.clearProperty(CHECK_SERVER_IDENTITY);
  }

  private void start() {
    appender.setContext(new LoggerContext());
    appender.setSMTPHost("localhost");
    appender.addTo("nospam@qos.ch");
    appender.start();
    assertTrue(appender.isStarted());
  }

  @Test
  public void checksServerIdentityWithSSL() {
    appender.setSSL(true);
    start();
    assertEquals("true", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void checksServerIdentityWithSTARTTLS() {
    appender.setSTARTTLS(true);
    start();
    assertEquals("true", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void systemPropertyOverridesServerIdentityCheck() {
    System.setProperty(CHECK_SERVER_IDENTITY, "false");
    appender.setSSL(true);
    start();
    assertEquals("false", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void plainSMTPLeavesServerIdentityCheckUnset() {
    start();
    assertNull(appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }
}
