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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.mock.MockAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.net.server.ServerSocketUtil;

/**
 * A functional test for {@link ServerSocketReceiver}.
 * <p>
 * In this test we create a SocketServer, connect to it over the local
 * network interface, and validate that it receives messages and delivers
 * them to its appender.
 */
public class ServerSocketReceiverFunctionalTest {

  private static final int EVENT_COUNT = 10;
  private static final int SHUTDOWN_DELAY = 10000;
  private MockAppender appender;
  private Logger logger;
  private ServerSocket serverSocket;
  private InstrumentedServerSocketReceiver receiver;
  private LoggerContext lc;

  @Before
  public void setUp() throws Exception {
    lc = new LoggerContext();

    appender = new MockAppender();
    appender.start();

    logger = lc.getLogger(getClass());
    logger.addAppender(appender);

    serverSocket = ServerSocketUtil.createServerSocket();
    receiver = new InstrumentedServerSocketReceiver(serverSocket);

    receiver.setContext(lc);
    receiver.start();
  }

  @After
  public void tearDown() throws Exception {
    receiver.stop();
    ExecutorService executor = lc.getScheduledExecutorService();
    executor.shutdownNow();
    executor.awaitTermination(SHUTDOWN_DELAY, TimeUnit.MILLISECONDS);
    assertTrue(executor.isTerminated());
  }

  @Test
  public void testLogEventFromClient() throws Exception {
    ILoggingEvent event = new LoggingEvent(logger.getName(), logger,
        Level.DEBUG, "test message", null, new Object[0]);
    Socket socket = new Socket(InetAddress.getLocalHost(),
        serverSocket.getLocalPort());

    try {
      LoggingEventVO eventVO = LoggingEventVO.build(event);

      ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
      for (int i = 0; i < EVENT_COUNT; i++) {
        oos.writeObject(eventVO);
      }

      oos.writeObject(eventVO);
      oos.flush();
    }
    finally {
      socket.close();
    }

    ILoggingEvent rcvdEvent = appender.awaitAppend(SHUTDOWN_DELAY);
    assertNotNull(rcvdEvent);
    assertEquals(event.getLoggerName(), rcvdEvent.getLoggerName());
    assertEquals(event.getLevel(), rcvdEvent.getLevel());
    assertEquals(event.getMessage(), rcvdEvent.getMessage());
  }

}
