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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class ConcurrentServerRunnerTest {

  private static final int DELAY = 10000;
  private static final int SHORT_DELAY = 10;

  private MockContext context = new MockContext();
  private MockServerListener<MockClient> listener =
      new MockServerListener<MockClient>();

  private ExecutorService executor = Executors.newCachedThreadPool();
  private InstrumentedConcurrentServerRunner runner =
      new InstrumentedConcurrentServerRunner(listener, executor);

  // collaborators of the tests below that call run() on the test thread
  private final ContextBase statusContext = new ContextBase();
  private final StatusChecker checker = new StatusChecker(statusContext);

  @SuppressWarnings("unchecked")
  private final ServerListener<RecordingClient> scriptedListener =
      mock(ServerListener.class);

  /** Tasks handed to {@link #capturingExecutor}, which doesn't run them. */
  private final List<Runnable> submitted = new ArrayList<Runnable>();
  private final Executor capturingExecutor = submitted::add;

  @Before
  public void setUp() throws Exception {
    runner.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
    // clear an interrupt a failed test may have left on the test thread
    Thread.interrupted();
    executor.shutdownNow();
    assertTrue(executor.awaitTermination(DELAY, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStartStop() throws Exception {
    assertFalse(runner.isRunning());
    executor.execute(runner);
    assertTrue(runner.awaitRunState(true, DELAY));
    int retries = DELAY / SHORT_DELAY;
    synchronized (listener) {
      while (retries-- > 0 && listener.getWaiter() == null) {
        listener.wait(SHORT_DELAY);
      }
    }
    assertNotNull(listener.getWaiter());
    runner.stop();
    assertTrue(listener.isClosed());
    assertFalse(runner.awaitRunState(false, DELAY));
  }

  @Test
  public void testRunOneClient() throws Exception {
    executor.execute(runner);
    MockClient client = new MockClient();
    listener.addClient(client);
    int retries = DELAY / SHORT_DELAY;
    synchronized (client) {
      while (retries-- > 0 && !client.isRunning()) {
        client.wait(SHORT_DELAY);
      }
    }
    assertTrue(runner.awaitRunState(true, DELAY));
    client.close();
    runner.stop();
  }

  @Test
  public void testRunManyClients() throws Exception {
    executor.execute(runner);
    int count = 10;
    while (count-- > 0) {
      MockClient client = new MockClient();
      listener.addClient(client);
      int retries = DELAY / SHORT_DELAY;
      synchronized (client) {
        while (retries-- > 0 && !client.isRunning()) {
          client.wait(SHORT_DELAY);
        }
      }
      assertTrue(runner.awaitRunState(true, DELAY));
    }
    runner.stop();
  }

  @Test
  public void testRunClientAndVisit() throws Exception {
    executor.execute(runner);
    MockClient client = new MockClient();
    listener.addClient(client);
    int retries = DELAY / SHORT_DELAY;
    synchronized (client) {
      while (retries-- > 0 && !client.isRunning()) {
        client.wait(SHORT_DELAY);
      }
    }
    assertTrue(runner.awaitRunState(true, DELAY));
    MockClientVisitor visitor = new MockClientVisitor();
    runner.accept(visitor);
    assertSame(client, visitor.getLastVisited());
    runner.stop();
  }

  @Test
  public void runExecutesEachAcceptedClientAndTracksItOnlyWhileItRuns()
      throws Exception {
    final SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, new Executor() {
          @Override
          public void execute(Runnable command) {
            command.run();
          }
        });
    selective.setContext(statusContext);
    final RecordingClient client = new RecordingClient("client-1");
    final List<RecordingClient> visitedWhileRunning =
        new ArrayList<RecordingClient>();
    final List<Boolean> runningStates = new ArrayList<Boolean>();
    client.onRun = () -> {
      runningStates.add(selective.isRunning());
      selective.accept(visitedWhileRunning::add);
    };
    when(scriptedListener.acceptClient())
        .thenReturn(client)
        .thenThrow(new InterruptedException());

    selective.run();

    assertEquals(1, client.runCount);
    assertEquals(Collections.singletonList(true), runningStates);
    assertEquals(Collections.singletonList(client), visitedWhileRunning);
    List<RecordingClient> visitedAfterwards = new ArrayList<RecordingClient>();
    selective.accept(visitedAfterwards::add);
    assertTrue(visitedAfterwards.isEmpty());
    assertFalse(selective.isRunning());
    verify(scriptedListener).close();
    checker.assertContainsMatch(Status.INFO, "listening on ");
    checker.assertContainsMatch(Status.INFO, "shutting down");
    checker.assertIsErrorFree();
  }

  @Test
  public void visitorFailureIsReportedAndTheOtherClientsAreStillVisited()
      throws Exception {
    final SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, capturingExecutor);
    selective.setContext(statusContext);
    final RecordingClient first = new RecordingClient("client-1");
    final RecordingClient second = new RecordingClient("client-2");
    when(scriptedListener.acceptClient())
        .thenReturn(first, second)
        .thenThrow(new InterruptedException());
    selective.run();
    assertEquals(2, submitted.size());

    // run the second client while the first one runs, so both are tracked
    // when the second one visits them
    final List<RecordingClient> visited = new ArrayList<RecordingClient>();
    first.onRun = submitted.get(1);
    second.onRun = () -> selective.accept(client -> {
      if (client == first) {
        throw new IllegalStateException("visit failed");
      }
      visited.add(client);
    });
    submitted.get(0).run();

    assertEquals(Collections.singletonList(second), visited);
    checker.assertContainsMatch(Status.ERROR,
        "client-1: java.lang.IllegalStateException: visit failed");
  }

  @Test
  public void clientThatCannotBeConfiguredIsDroppedAndClosed()
      throws Exception {
    SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, capturingExecutor);
    selective.setContext(statusContext);
    RecordingClient dropped = new RecordingClient("client-1");
    dropped.configurable = false;
    RecordingClient accepted = new RecordingClient("client-2");
    when(scriptedListener.acceptClient())
        .thenReturn(dropped, accepted)
        .thenThrow(new InterruptedException());

    selective.run();

    assertTrue(dropped.closed);
    checker.assertContainsMatch(Status.ERROR, "client-1: connection dropped");
    checker.assertNoMatch("client-2: connection dropped");
    assertEquals(1, submitted.size());
    submitted.get(0).run();
    assertEquals(0, dropped.runCount);
    assertEquals(1, accepted.runCount);
    assertFalse(accepted.closed);
  }

  @Test
  public void clientRejectedByTheExecutorIsDroppedAndClosed()
      throws Exception {
    Executor rejectsFirstClient = new Executor() {
      private boolean rejected;

      @Override
      public void execute(Runnable command) {
        if (!rejected) {
          rejected = true;
          throw new RejectedExecutionException("pool exhausted");
        }
        submitted.add(command);
      }
    };
    SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, rejectsFirstClient);
    selective.setContext(statusContext);
    RecordingClient rejected = new RecordingClient("client-1");
    RecordingClient accepted = new RecordingClient("client-2");
    when(scriptedListener.acceptClient())
        .thenReturn(rejected, accepted)
        .thenThrow(new InterruptedException());

    selective.run();

    assertTrue(rejected.closed);
    checker.assertContainsMatch(Status.ERROR, "client-1: connection dropped");
    assertEquals(1, submitted.size());
    submitted.get(0).run();
    assertEquals(0, rejected.runCount);
    assertEquals(1, accepted.runCount);
    assertFalse(accepted.closed);
  }

  @Test
  public void interruptingTheRunnerThreadStopsAcceptingClients()
      throws Exception {
    SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, capturingExecutor);
    selective.setContext(statusContext);
    final RecordingClient client = new RecordingClient("client-1");
    when(scriptedListener.acceptClient())
        .thenAnswer(invocation -> {
          Thread.currentThread().interrupt();
          return client;
        })
        .thenThrow(new IOException("accepted after the interrupt"));

    try {
      selective.run();
      assertTrue(Thread.currentThread().isInterrupted());
    }
    finally {
      Thread.interrupted();
    }

    verify(scriptedListener, times(1)).acceptClient();
    assertEquals(1, submitted.size());
    assertFalse(selective.isRunning());
    verify(scriptedListener).close();
    checker.assertContainsMatch(Status.INFO, "shutting down");
    checker.assertIsErrorFree();
  }

  @Test
  public void listenerFailureIsReportedAndStopsTheRunner() throws Exception {
    SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, capturingExecutor);
    selective.setContext(statusContext);
    when(scriptedListener.acceptClient())
        .thenThrow(new IOException("listener failed"));

    selective.run();

    checker.assertContainsMatch(Status.ERROR,
        "listener: java.io.IOException: listener failed");
    assertFalse(selective.isRunning());
    verify(scriptedListener).close();
    assertTrue(submitted.isEmpty());
  }

  @Test
  public void closingAnExecutedClientTaskClosesTheClient() throws Exception {
    SelectiveServerRunner selective =
        new SelectiveServerRunner(scriptedListener, capturingExecutor);
    selective.setContext(statusContext);
    RecordingClient client = new RecordingClient("client-1");
    when(scriptedListener.acceptClient())
        .thenReturn(client)
        .thenThrow(new InterruptedException());
    selective.run();
    assertEquals(1, submitted.size());
    assertTrue(submitted.get(0) instanceof Client);

    ((Client) submitted.get(0)).close();

    assertTrue(client.closed);
    assertEquals(0, client.runCount);
  }

  /**
   * A {@link ConcurrentServerRunner} that configures only the clients that
   * are {@link RecordingClient#configurable}.
   */
  private static class SelectiveServerRunner
      extends ConcurrentServerRunner<RecordingClient> {

    SelectiveServerRunner(ServerListener<RecordingClient> listener,
        Executor executor) {
      super(listener, executor);
    }

    @Override
    protected boolean configureClient(RecordingClient client) {
      return client.configurable;
    }
  }

  /**
   * A {@link Client} that records how it is used, and whose {@link #run()}
   * returns as soon as its {@link #onRun} task is done.
   */
  private static class RecordingClient implements Client {

    private final String name;
    boolean configurable = true;
    Runnable onRun = () -> { };
    int runCount;
    boolean closed;

    RecordingClient(String name) {
      this.name = name;
    }

    @Override
    public void run() {
      runCount++;
      onRun.run();
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  static class InstrumentedConcurrentServerRunner
      extends ConcurrentServerRunner<MockClient> {

    private final Lock lock = new ReentrantLock();
    private final Condition runningCondition = lock.newCondition();

    public InstrumentedConcurrentServerRunner(
        ServerListener<MockClient> listener, Executor executor) {
      super(listener, executor);
    }

    @Override
    protected boolean configureClient(MockClient client) {
      return true;
    }

    @Override
    protected void setRunning(boolean running) {
      lock.lock();
      try {
        super.setRunning(running);
        runningCondition.signalAll();
      }
      finally {
        lock.unlock();
      }
    }

    public boolean awaitRunState(boolean state,
        long delay) throws InterruptedException {
      lock.lock();
      try {
        while (isRunning() != state) {
          runningCondition.await(delay, TimeUnit.MILLISECONDS);
        }
        return isRunning();
      }
      finally {
        lock.unlock();
      }
    }
  }

}
