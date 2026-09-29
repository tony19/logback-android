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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.SocketConnector.ExceptionHandler;
import ch.qos.logback.core.net.SocketConnectorBase.DelayStrategy;

/**
 * Unit tests for {@link SocketConnectorBase}.
 *
 * @author Carl Harris
 */
public class SocketConnectorBaseTest {

  /** Upper bound for a test that involves another thread; never reached when it passes. */
  private static final long TIMEOUT = 10000;

  private static final int PORT = 4560;

  private final SocketFactory socketFactory = mock(SocketFactory.class);
  private final ExceptionHandler exceptionHandler = mock(ExceptionHandler.class);
  private final DelayStrategy noDelay = mock(DelayStrategy.class);

  private InetAddress address;
  private Socket socket;
  private ServerSocket serverSocket;

  @Before
  public void setUp() throws Exception {
    address = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
    socket = new Socket();
  }

  @After
  public void tearDown() throws Exception {
    // clear an interrupt a failed test may have left behind
    Thread.interrupted();
    socket.close();
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  @Test
  public void connectsToTheConfiguredAddressAndPort() throws Exception {
    when(socketFactory.createSocket(address, PORT)).thenReturn(socket);
    SocketConnectorBase connector = newConnector();

    connector.run();

    assertSame(socket, connector.awaitConnection(0));
    verify(noDelay, times(1)).nextDelay();
    verifyNoInteractions(exceptionHandler);
  }

  @Test
  public void cannotBeReusedOnceConnected() throws Exception {
    when(socketFactory.createSocket(address, PORT)).thenReturn(socket);
    SocketConnectorBase connector = newConnector();
    connector.run();

    IllegalStateException e = assertThrows(IllegalStateException.class, connector::run);

    assertEquals("connector cannot be reused", e.getMessage());
    verify(socketFactory, times(1)).createSocket(address, PORT);
  }

  @Test
  public void retriesAfterFailedAttemptUntilConnected() throws Exception {
    ConnectException failure = new ConnectException("refused");
    when(socketFactory.createSocket(address, PORT)).thenThrow(failure).thenReturn(socket);
    SocketConnectorBase connector = newConnector();

    connector.run();

    assertSame(socket, connector.awaitConnection(0));
    verify(exceptionHandler).connectionFailed(connector, failure);
    verify(noDelay, times(2)).nextDelay();
  }

  @Test
  public void reportsInterruptionWhileWaitingToConnect() throws Exception {
    DelayStrategy interruptingDelay = () -> {
      Thread.currentThread().interrupt();
      return 1;
    };
    SocketConnectorBase connector = new SocketConnectorBase(address, PORT, interruptingDelay);
    connector.setSocketFactory(socketFactory);
    connector.setExceptionHandler(exceptionHandler);

    connector.run();

    verify(exceptionHandler).connectionFailed(eq(connector), any(InterruptedException.class));
    verifyNoInteractions(socketFactory);
    assertNull(connector.awaitConnection(0));
  }

  @Test
  public void doesNotConnectWhenAlreadyInterrupted() throws Exception {
    SocketConnectorBase connector = newConnector();

    boolean interrupted;
    Thread.currentThread().interrupt();
    try {
      connector.run();
    } finally {
      interrupted = Thread.interrupted();
    }

    assertTrue(interrupted);
    verifyNoInteractions(noDelay, socketFactory, exceptionHandler);
  }

  @Test
  public void stopsRetryingWhenInterruptedAfterFailedAttempt() throws Exception {
    final ConnectException failure = new ConnectException("refused");
    when(socketFactory.createSocket(address, PORT)).thenAnswer(invocation -> {
      Thread.currentThread().interrupt();
      throw failure;
    });
    SocketConnectorBase connector = newConnector();

    boolean interrupted;
    try {
      connector.run();
    } finally {
      interrupted = Thread.interrupted();
    }

    assertTrue(interrupted);
    verify(exceptionHandler).connectionFailed(connector, failure);
    verify(socketFactory, times(1)).createSocket(address, PORT);
    verify(noDelay, times(1)).nextDelay();
  }

  @Test
  public void reportsFailuresOnTheConsoleWithoutExceptionHandler() throws Exception {
    when(socketFactory.createSocket(address, PORT))
        .thenThrow(new ConnectException("refused (test)")).thenReturn(socket);
    SocketConnectorBase connector = new SocketConnectorBase(address, PORT, noDelay);
    connector.setSocketFactory(socketFactory);

    PrintStream originalOut = System.out;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, "UTF-8"));
    try {
      connector.run();
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString("UTF-8").contains("java.net.ConnectException: refused (test)"));
    assertSame(socket, connector.awaitConnection(0));
  }

  @Test(timeout = TIMEOUT)
  public void usesThePlatformSocketFactoryByDefault() throws Exception {
    serverSocket = new ServerSocket(0, 1, address);
    SocketConnectorBase connector = new SocketConnectorBase(address, serverSocket.getLocalPort(), noDelay);
    connector.setExceptionHandler(exceptionHandler);

    connector.run();

    Socket connected = connector.awaitConnection(0);
    try {
      assertTrue(connected.isConnected());
      assertEquals(serverSocket.getLocalPort(), connected.getPort());
    } finally {
      connected.close();
    }
    verifyNoInteractions(exceptionHandler);
  }

  @Test
  public void awaitConnectionReturnsNullWhenNotConnectedInTime() throws Exception {
    SocketConnectorBase connector = newConnector();

    assertNull(connector.awaitConnection(1));
  }

  @Test(timeout = TIMEOUT)
  public void awaitConnectionReturnsTheSocketOnceConnected() throws Exception {
    final Thread awaitingThread = Thread.currentThread();
    // connect only once the awaiting thread waits for the connection
    when(socketFactory.createSocket(address, PORT)).thenAnswer(invocation -> {
      while (awaitingThread.getState() != Thread.State.TIMED_WAITING) {
        Thread.yield();
      }
      return socket;
    });
    SocketConnectorBase connector = newConnector();
    Thread connectorThread = new Thread(connector::run);
    connectorThread.start();

    Socket connected;
    try {
      connected = connector.awaitConnection();
    } finally {
      connectorThread.join();
    }

    assertSame(socket, connected);
  }

  @Test
  public void waitsTheInitialDelayFirstAndTheRetryDelayAfterwards() throws Exception {
    SocketConnectorBase connector = new SocketConnectorBase(address, PORT, 3, 7);

    DelayStrategy delayStrategy = delayStrategyOf(connector);

    assertEquals(3, delayStrategy.nextDelay());
    assertEquals(7, delayStrategy.nextDelay());
    assertEquals(7, delayStrategy.nextDelay());
  }

  @Test
  public void callReturnsNoSocket() throws Exception {
    SocketConnectorBase connector = newConnector();

    assertNull(connector.call());
    verifyNoInteractions(socketFactory);
  }

  private SocketConnectorBase newConnector() {
    SocketConnectorBase connector = new SocketConnectorBase(address, PORT, noDelay);
    connector.setSocketFactory(socketFactory);
    connector.setExceptionHandler(exceptionHandler);
    return connector;
  }

  private static DelayStrategy delayStrategyOf(SocketConnectorBase connector) throws Exception {
    Field field = SocketConnectorBase.class.getDeclaredField("delayStrategy");
    field.setAccessible(true);
    return (DelayStrategy) field.get(connector);
  }

}
