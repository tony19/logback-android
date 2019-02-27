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
package ch.qos.logback.classic.net.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.net.ServerSocketFactory;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;

/**
 * Unit tests for {@link SSLServerSocketReceiver}.
 *
 * @author Carl Harris
 */
public class SSLServerSocketReceiverTest {

  private MockContext context = new MockContext();

  private MockSSLConfiguration ssl = new MockSSLConfiguration();

  private MockSSLParametersConfiguration parameters =
      new MockSSLParametersConfiguration();

  private SSLServerSocketReceiver receiver = new SSLServerSocketReceiver();

  @Before
  public void setUp() throws Exception {
    receiver.setContext(context);
    receiver.setSsl(ssl);
    ssl.setParameters(parameters);
  }

  @Test
  public void testGetServerSocketFactory() throws Exception {
    ServerSocketFactory socketFactory = receiver.getServerSocketFactory();
    assertNotNull(socketFactory);
    assertTrue(ssl.isContextCreated());
    assertTrue(parameters.isContextInjected());
  }

}
