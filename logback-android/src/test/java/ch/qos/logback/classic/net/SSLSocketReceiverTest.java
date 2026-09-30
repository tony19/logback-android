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
package ch.qos.logback.classic.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.status.Status;

/**
 * Unit tests for {@link SSLSocketReceiver}.
 *
 * @author Carl Harris
 */
public class SSLSocketReceiverTest {

  private final LoggerContext lc = new LoggerContext();

  private SSLSocketReceiver remote =
      new SSLSocketReceiver();

  @Before
  public void setUp() throws Exception {
    remote.setContext(lc);
  }

  @After
  public void tearDown() {
    remote.stop();
    lc.stop();
  }

  @Test
  public void testUsingDefaultConfig() throws Exception {
    // should be able to start successfully with no SSL configuration at all
    remote.setRemoteHost(InetAddress.getLocalHost().getHostAddress());
    remote.setPort(6000);
    remote.start();
    assertNotNull(remote.getSocketFactory());
  }

  @Test
  public void startFailsWhenTheSslContextCannotBeCreated() throws Exception {
    NoSuchAlgorithmException failure = new NoSuchAlgorithmException("no TLS here");
    SSLConfiguration ssl = mock(SSLConfiguration.class);
    when(ssl.createContext(any(ContextAware.class))).thenThrow(failure);
    remote.setSsl(ssl);
    remote.setRemoteHost("127.0.0.1");
    remote.setPort(6000);

    remote.start();

    assertFalse(remote.isStarted());
    assertNull(remote.getSocketFactory());
    List<Status> statuses = lc.getStatusManager().getCopyOfStatusList();
    Status last = statuses.get(statuses.size() - 1);
    assertEquals(Status.ERROR, last.getLevel());
    assertEquals("no TLS here", last.getMessage());
    assertSame(failure, last.getThrowable());
  }

  @Test
  public void sslConfigurationCanBeReplaced() {
    SSLConfiguration ssl = new SSLConfiguration();
    remote.setSsl(ssl);
    assertSame(ssl, remote.getSsl());
  }
}
