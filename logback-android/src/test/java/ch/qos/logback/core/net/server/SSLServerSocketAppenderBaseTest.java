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
package ch.qos.logback.core.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.ExecutorServiceUtil;

/**
 * Unit tests for {@link SSLServerSocketAppenderBase}.
 *
 * @author Carl Harris
 */
public class SSLServerSocketAppenderBaseTest {

  private MockContext context = new MockContext(ExecutorServiceUtil.newScheduledExecutorService());

  private SSLServerSocketAppenderBase appender =
      new InstrumentedSSLServerSocketAppenderBase();

  @Before
  public void setUp() throws Exception {
    appender.setContext(context);
  }

  @Test
  public void testUsingDefaultConfig() throws Exception {
    // should be able to start successfully with no SSL configuration at all
    appender.start();
    assertNotNull(appender.getServerSocketFactory());
    appender.stop();
  }

  @Test
  public void defaultSslConfigurationIsCreatedOnceAndCanBeReplaced() {
    SSLConfiguration defaultSsl = appender.getSsl();
    assertNotNull(defaultSsl);
    assertSame(defaultSsl, appender.getSsl());

    SSLConfiguration ssl = new SSLConfiguration();
    appender.setSsl(ssl);

    assertSame(ssl, appender.getSsl());
  }

  @Test
  public void sslSetupFailureIsReportedAndLeavesTheAppenderStopped() {
    SSLConfiguration ssl = new SSLConfiguration();
    ssl.setProtocol("A_FAKE_PROTOCOL_NAME");
    appender.setSsl(ssl);

    appender.start();

    assertFalse(appender.isStarted());
    assertNull(appender.getServerSocketFactory());
    Status status = context.getLastStatus();
    assertTrue(status instanceof ErrorStatus);
    assertTrue(status.getThrowable() instanceof NoSuchAlgorithmException);
    assertEquals(status.getThrowable().getMessage(), status.getMessage());
    assertTrue(status.getMessage().contains("A_FAKE_PROTOCOL_NAME"));
  }

  private static class InstrumentedSSLServerSocketAppenderBase
      extends SSLServerSocketAppenderBase<Object> {

    @Override
    protected void postProcessEvent(Object event) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected PreSerializationTransformer<Object> getPST() {
      throw new UnsupportedOperationException();
    }

  }

}
