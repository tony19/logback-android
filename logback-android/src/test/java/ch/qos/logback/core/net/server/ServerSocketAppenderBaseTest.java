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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ServerSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Unit tests for {@link ServerSocketAppenderBase}.
 *
 * @author Carl Harris
 */
public class ServerSocketAppenderBaseTest {

  private final ScheduledExecutorService executor =
      mock(ScheduledExecutorService.class);

  private final ContextBase context = new ContextBase() {
    @Override
    public synchronized ScheduledExecutorService getScheduledExecutorService() {
      return executor;
    }
  };

  private final StatusChecker checker = new StatusChecker(context);

  private final ServerSocketFactory socketFactory =
      mock(ServerSocketFactory.class);

  @SuppressWarnings("unchecked")
  private final ServerRunner<RemoteReceiverClient> runner =
      mock(ServerRunner.class);

  private final RecordingServerSocketAppender appender =
      new RecordingServerSocketAppender();

  private InetAddress loopback;
  private ServerSocket serverSocket;

  @Before
  public void setUp() throws Exception {
    loopback = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
    serverSocket = new ServerSocket();
    when(socketFactory.createServerSocket(anyInt(), anyInt(), any()))
        .thenReturn(serverSocket);
    appender.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    serverSocket.close();
    if (appender.lastSocket != null) {
      appender.lastSocket.close();
    }
  }

  @Test
  public void listenerSettingsHaveDefaultsAndCanBeChanged() {
    assertEquals(AbstractSocketAppender.DEFAULT_PORT, appender.getPort());
    assertEquals(ServerSocketAppenderBase.DEFAULT_BACKLOG,
        appender.getBacklog().intValue());
    assertEquals(ServerSocketAppenderBase.DEFAULT_CLIENT_QUEUE_SIZE,
        appender.getClientQueueSize());
    assertNull(appender.getAddress());

    appender.setPort(4561);
    appender.setBacklog(7);
    appender.setClientQueueSize(3);
    appender.setAddress("127.0.0.1");

    assertEquals(4561, appender.getPort());
    assertEquals(7, appender.getBacklog().intValue());
    assertEquals(3, appender.getClientQueueSize());
    assertEquals("127.0.0.1", appender.getAddress());
  }

  @Test
  public void defaultServerSocketFactoryIsThePlatformDefault()
      throws Exception {
    assertSame(ServerSocketFactory.getDefault(),
        appender.getServerSocketFactory());
  }

  @Test
  public void inetAddressIsNullWhenNoAddressIsSet() throws Exception {
    assertNull(appender.getInetAddress());
  }

  @Test
  public void inetAddressIsResolvedFromTheConfiguredAddress() throws Exception {
    appender.setAddress("127.0.0.1");
    assertEquals(loopback, appender.getInetAddress());
  }

  @Test
  public void startCreatesTheServerSocketFromTheListenerSettings()
      throws Exception {
    appender.socketFactory = socketFactory;
    appender.setPort(4561);
    appender.setBacklog(7);
    appender.setAddress("127.0.0.1");

    appender.start();

    assertTrue(appender.isStarted());
    verify(socketFactory).createServerSocket(4561, 7, loopback);
    assertSame(serverSocket, appender.lastSocket);
    assertTrue(appender.lastListener instanceof RemoteReceiverServerListener);
    assertSame(executor, appender.lastExecutor);
  }

  @Test
  public void startRunsTheDefaultServerRunnerOnTheContextExecutor()
      throws Exception {
    appender.socketFactory = socketFactory;

    appender.start();

    ArgumentCaptor<Runnable> command = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(command.capture());
    assertTrue(command.getValue() instanceof RemoteReceiverServerRunner);
    RemoteReceiverServerRunner defaultRunner =
        (RemoteReceiverServerRunner) command.getValue();
    assertSame(context, defaultRunner.getContext());
    assertFalse(defaultRunner.isRunning());
  }

  @Test
  public void startListensOnTheConfiguredAddressAndStopClosesTheSocket()
      throws Exception {
    appender.setAddress("127.0.0.1");
    appender.setPort(0);

    appender.start();

    assertTrue(appender.isStarted());
    assertTrue(appender.lastSocket.isBound());
    assertEquals(loopback, appender.lastSocket.getInetAddress());

    appender.stop();

    assertFalse(appender.isStarted());
    assertTrue(appender.lastSocket.isClosed());
  }

  @Test
  public void startWhenAlreadyStartedDoesNothing() throws Exception {
    appender.socketFactory = socketFactory;
    appender.runner = runner;

    appender.start();
    appender.start();

    verify(socketFactory, times(1))
        .createServerSocket(anyInt(), anyInt(), any());
    verify(executor, times(1)).execute(runner);
  }

  @Test
  public void startHandsTheContextToTheRunnerBeforeExecutingIt()
      throws Exception {
    appender.socketFactory = socketFactory;
    appender.runner = runner;

    appender.start();

    verify(runner).setContext(context);
    verify(executor).execute(runner);
  }

  @Test
  public void startFailureIsReportedAndLeavesTheAppenderStopped()
      throws Exception {
    BindException ex = new BindException("Address already in use");
    when(socketFactory.createServerSocket(anyInt(), anyInt(), any()))
        .thenThrow(ex);
    appender.socketFactory = socketFactory;

    appender.start();

    assertFalse(appender.isStarted());
    checker.assertContainsMatch(Status.ERROR,
        "server startup error: java.net.BindException: Address already in use");
    assertTrue(checker.containsException(BindException.class));
    verifyNoInteractions(executor);
  }

  @Test
  public void stopStopsTheServerRunner() throws Exception {
    appender.socketFactory = socketFactory;
    appender.runner = runner;
    appender.start();

    appender.stop();

    verify(runner).stop();
    assertFalse(appender.isStarted());
  }

  @Test
  public void stopWhenNotStartedDoesNothing() throws Exception {
    appender.runner = runner;

    appender.stop();

    assertFalse(appender.isStarted());
    verifyNoInteractions(runner);
  }

  @Test
  public void stopFailureIsReportedAndLeavesTheAppenderStarted()
      throws Exception {
    IOException ex = new IOException("test exception");
    doThrow(ex).when(runner).stop();
    appender.socketFactory = socketFactory;
    appender.runner = runner;
    appender.start();

    appender.stop();

    assertTrue(appender.isStarted());
    checker.assertContainsMatch(Status.ERROR,
        "server shutdown error: java.io.IOException: test exception");
    assertTrue(checker.containsException(IOException.class));
  }

  @Test
  public void appendIgnoresNullEvents() throws Exception {
    appender.socketFactory = socketFactory;
    appender.runner = runner;
    appender.start();

    appender.doAppend(null);

    assertTrue(appender.postProcessed.isEmpty());
    verify(runner, never()).accept(any());
    checker.assertIsErrorFree();
  }

  @Test
  public void appendOffersTheTransformedEventToEachClient() throws Exception {
    final RemoteReceiverClient client1 = mock(RemoteReceiverClient.class);
    final RemoteReceiverClient client2 = mock(RemoteReceiverClient.class);
    doAnswer(invocation -> {
      ClientVisitor<RemoteReceiverClient> visitor = invocation.getArgument(0);
      visitor.visit(client1);
      visitor.visit(client2);
      return null;
    }).when(runner).accept(any());
    appender.socketFactory = socketFactory;
    appender.runner = runner;
    appender.start();

    appender.doAppend("event");

    assertEquals(1, appender.postProcessed.size());
    assertEquals("event", appender.postProcessed.get(0));
    verify(client1).offer("serialized event");
    verify(client2).offer("serialized event");
  }

  @Test
  public void defaultServerRunnerGivesEachClientAQueueOfTheConfiguredSize() {
    appender.setClientQueueSize(2);
    @SuppressWarnings("unchecked")
    ServerListener<RemoteReceiverClient> listener = mock(ServerListener.class);

    ServerRunner<RemoteReceiverClient> defaultRunner =
        appender.createServerRunner(listener, executor);

    assertTrue(defaultRunner instanceof RemoteReceiverServerRunner);
    RemoteReceiverStreamClient client =
        new RemoteReceiverStreamClient("id", new ByteArrayOutputStream());
    assertTrue(((RemoteReceiverServerRunner) defaultRunner)
        .configureClient(client));
    assertTrue(client.offer("1"));
    assertTrue(client.offer("2"));
    assertFalse(client.offer("3"));
  }

  /**
   * A {@link ServerSocketAppenderBase} that records what it is given, and
   * uses the base class's socket factory and server runner unless a
   * replacement is set.
   */
  private static class RecordingServerSocketAppender
      extends ServerSocketAppenderBase<String> {

    final List<String> postProcessed = new ArrayList<String>();
    ServerSocketFactory socketFactory;
    ServerRunner<RemoteReceiverClient> runner;
    ServerSocket lastSocket;
    ServerListener<RemoteReceiverClient> lastListener;
    Executor lastExecutor;

    @Override
    protected void postProcessEvent(String event) {
      postProcessed.add(event);
    }

    @Override
    protected PreSerializationTransformer<String> getPST() {
      return new PreSerializationTransformer<String>() {
        @Override
        public Serializable transform(String event) {
          return "serialized " + event;
        }
      };
    }

    @Override
    protected ServerSocketFactory getServerSocketFactory() throws Exception {
      return socketFactory != null
          ? socketFactory : super.getServerSocketFactory();
    }

    @Override
    protected ServerListener<RemoteReceiverClient> createServerListener(
        ServerSocket socket) {
      lastSocket = socket;
      return super.createServerListener(socket);
    }

    @Override
    protected ServerRunner<RemoteReceiverClient> createServerRunner(
        ServerListener<RemoteReceiverClient> listener, Executor executor) {
      lastListener = listener;
      lastExecutor = executor;
      return runner != null
          ? runner : super.createServerRunner(listener, executor);
    }
  }

}
