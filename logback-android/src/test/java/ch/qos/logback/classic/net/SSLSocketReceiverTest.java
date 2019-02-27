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

import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * Unit tests for {@link SSLSocketReceiver}.
 *
 * @author Carl Harris
 */
@RunWith(RobolectricTestRunner.class)
public class SSLSocketReceiverTest {

  private SSLSocketReceiver remote =
      new SSLSocketReceiver();

  @Before
  public void setUp() throws Exception {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    remote.setContext(lc);
  }

  @Test
  public void testUsingDefaultConfig() throws Exception {
    // should be able to start successfully with no SSL configuration at all
    remote.setRemoteHost(InetAddress.getLocalHost().getHostAddress());
    remote.setPort(6000);
    remote.start();
    assertNotNull(remote.getSocketFactory());
  }
}
