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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;

public class ConcurrentServerRunnerTest {

  private static final int DELAY = 10000;
  private static final int SHORT_DELAY = 10;

  private MockContext context = new MockContext();
  private MockServerListener<MockClient> listener =
      new MockServerListener<MockClient>();

  private ExecutorService executor = Executors.newCachedThreadPool();
  private InstrumentedConcurrentServerRunner runner =
      new InstrumentedConcurrentServerRunner(listener, executor);

  @Before
  public void setUp() throws Exception {
    runner.setContext(context);
  }

  @After
  public void tearDown() throws Exception {
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
