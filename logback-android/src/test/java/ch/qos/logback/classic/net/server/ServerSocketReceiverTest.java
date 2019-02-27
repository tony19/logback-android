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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.MockServerListener;
import ch.qos.logback.core.net.server.MockServerRunner;
import ch.qos.logback.core.net.server.ServerSocketUtil;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

/**
 * Unit tests for {@link ServerSocketReceiver}.
 *
 * @author Carl Harris
 */
public class ServerSocketReceiverTest {

  private MockContext context = new MockContext();

  private MockServerRunner<RemoteAppenderClient> runner =
      new MockServerRunner<RemoteAppenderClient>();

  private MockServerListener<RemoteAppenderClient> listener =
      new MockServerListener<RemoteAppenderClient>();

  private ServerSocket serverSocket;
  private InstrumentedServerSocketReceiver receiver;

  @Before
  public void setUp() throws Exception {
    serverSocket = ServerSocketUtil.createServerSocket();
    receiver = new InstrumentedServerSocketReceiver(serverSocket, listener, runner);
    receiver.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    serverSocket.close();
  }

  @Test
  public void testStartStop() throws Exception {
    receiver.start();
    assertTrue(runner.isContextInjected());
    assertTrue(runner.isRunning());
    assertSame(listener, receiver.getLastListener());

    receiver.stop();
    assertFalse(runner.isRunning());
  }

  @Test
  public void testStartWhenAlreadyStarted() throws Exception {
    receiver.start();
    receiver.start();
    assertEquals(1, runner.getStartCount());
  }

  @Test
  public void testStopThrowsException() throws Exception {
    receiver.start();
    assertTrue(receiver.isStarted());
    IOException ex = new IOException("test exception");
    runner.setStopException(ex);
    receiver.stop();

    Status status = context.getLastStatus();
    assertNotNull(status);
    assertTrue(status instanceof ErrorStatus);
    assertTrue(status.getMessage().contains(ex.getMessage()));
    assertSame(ex, status.getThrowable());
  }

  @Test
  public void testStopWhenNotStarted() throws Exception {
    receiver.stop();
    assertEquals(0, runner.getStartCount());
  }

}
