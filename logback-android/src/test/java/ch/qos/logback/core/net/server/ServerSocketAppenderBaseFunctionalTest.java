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
import static org.junit.Assert.assertTrue;

import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.util.ExecutorServiceUtil;

/**
 * A functional test for {@link AbstractServerSocketAppender}.
 *
 * @author Carl Harris
 */
public class ServerSocketAppenderBaseFunctionalTest {

  private static final String TEST_EVENT = "test event";

  private static final int EVENT_COUNT = 10;

  private ScheduledExecutorService executor = ExecutorServiceUtil.newScheduledExecutorService();
  private MockContext context = new MockContext(executor);
  private ServerSocket serverSocket;
  private InstrumentedServerSocketAppenderBase appender;

  @Before
  public void setUp() throws Exception {

    serverSocket = ServerSocketUtil.createServerSocket();

    appender = new InstrumentedServerSocketAppenderBase(serverSocket);
    appender.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    executor.shutdownNow();
    executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
    assertTrue(executor.isTerminated());
  }

  @Test
  public void testLogEventClient() throws Exception {
    appender.start();
    Socket socket = new Socket(InetAddress.getLocalHost(),
        serverSocket.getLocalPort());

    socket.setSoTimeout(1000);
    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

    for (int i = 0; i < EVENT_COUNT; i++) {
      appender.append(TEST_EVENT + i);
      assertEquals(TEST_EVENT + i, ois.readObject());
    }

    socket.close();
    appender.stop();
  }

}
