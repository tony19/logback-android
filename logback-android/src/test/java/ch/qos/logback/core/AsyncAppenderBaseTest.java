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
package ch.qos.logback.core;

import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.testUtil.DelayingListAppender;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.NPEAppender;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Ceki G&uuml;lc&uuml;
 * @author Torsten Juergeleit
 */
public class AsyncAppenderBaseTest {


  Context context = new ContextBase();
  AsyncAppenderBase<Integer> asyncAppenderBase = new AsyncAppenderBase<Integer>();
  LossyAsyncAppender lossyAsyncAppender = new LossyAsyncAppender();
  DelayingListAppender<Integer> delayingListAppender = new DelayingListAppender<Integer>();
  ListAppender<Integer> listAppender = new ListAppender<Integer>();
  StatusChecker statusChecker = new StatusChecker(context);

  @Before
  public void setUp() {
    asyncAppenderBase.setContext(context);
    lossyAsyncAppender.setContext(context);

    listAppender.setContext(context);
    listAppender.setName("list");
    listAppender.start();

    delayingListAppender.setContext(context);
    delayingListAppender.setName("list");
    delayingListAppender.start();
  }

  @Test(timeout = 2000)
  public void smoke() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    asyncAppenderBase.stop();
    verify(listAppender, 1);
  }

  @Test
  public void exceptionsShouldNotCauseHalting() throws InterruptedException {
    NPEAppender<Integer> npeAppender = new NPEAppender<Integer>();
    npeAppender.setName("bad");
    npeAppender.setContext(context);
    npeAppender.start();

    asyncAppenderBase.addAppender(npeAppender);
    asyncAppenderBase.start();
    assertTrue(asyncAppenderBase.isStarted());
    for (int i = 0; i < 10; i++)
      asyncAppenderBase.append(i);

    asyncAppenderBase.stop();
    assertFalse(asyncAppenderBase.isStarted());
    assertEquals(AppenderBase.ALLOWED_REPEATS, statusChecker.matchCount("Appender \\[bad\\] failed to append."));
  }

  @Test(timeout = 2000)
  public void emptyQueueShouldBeStoppable() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.stop();
    verify(listAppender, 0);
  }

  @Test(timeout = 2000)
  public void workerShouldStopEvenIfInterruptExceptionConsumedWithinSubappender() {
    delayingListAppender.delay = 100;
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    asyncAppenderBase.stop();
    verify(delayingListAppender, 1);
    assertTrue(delayingListAppender.interrupted);
    Thread.interrupted();
  }

  @Test(timeout = 2000)
  public void noEventLoss() {
    int bufferSize = 10;
    int loopLen = bufferSize * 2;
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.setQueueSize(bufferSize);
    asyncAppenderBase.start();
    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    asyncAppenderBase.stop();
    verify(delayingListAppender, loopLen);
  }

  @Test(timeout = 2000)
   public void eventLossIfNeverBlock() {
    int bufferSize = 10;
    int loopLen = bufferSize * 2;
    delayingListAppender.setDelay(5000); // something greater than the test timeout
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.setQueueSize(bufferSize);
    asyncAppenderBase.setNeverBlock(true);
    asyncAppenderBase.start();
    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    asyncAppenderBase.stop();
    // ListAppender size isn't a reliable test here, so just make sure we didn't
    // have any errors, and that we could complete the test in time.
    statusChecker.assertIsErrorFree();
  }

  @Test(timeout = 2000)
  public void lossyAppenderShouldOnlyLoseCertainEvents() {
    int bufferSize = 5;
    int loopLen = bufferSize * 2;
    lossyAsyncAppender.addAppender(delayingListAppender);
    lossyAsyncAppender.setQueueSize(bufferSize);
    lossyAsyncAppender.setDiscardingThreshold(1);
    lossyAsyncAppender.start();
    for (int i = 0; i < loopLen; i++) {
      lossyAsyncAppender.doAppend(i);
    }
    lossyAsyncAppender.stop();
    // events 0, 3, 6 and 9 are discardable. However, for events 0 and 3
    // the buffer is not not yet full. Thus, only events 6 and 9 will be
    // effectively discarded.
    verify(delayingListAppender, loopLen - 2);
  }

  @Test(timeout = 2000)
  public void lossyAppenderShouldBeNonLossyIfDiscardingThresholdIsZero() {
    int bufferSize = 5;
    int loopLen = bufferSize * 2;
    lossyAsyncAppender.addAppender(delayingListAppender);
    lossyAsyncAppender.setQueueSize(bufferSize);
    lossyAsyncAppender.setDiscardingThreshold(0);
    lossyAsyncAppender.start();
    for (int i = 0; i < loopLen; i++) {
      lossyAsyncAppender.doAppend(i);
    }
    lossyAsyncAppender.stop();
    verify(delayingListAppender, loopLen);
  }

  @Test
  public void invalidQueueCapacityShouldResultInNonStartedAppender() {
    asyncAppenderBase.addAppender(new NOPAppender<Integer>());
    asyncAppenderBase.setQueueSize(0);
    assertEquals(0, asyncAppenderBase.getQueueSize());
    asyncAppenderBase.start();
    assertFalse(asyncAppenderBase.isStarted());
    statusChecker.assertContainsMatch("Invalid queue size");
  }

  @Test
  public void workerThreadFlushesOnStop() throws InterruptedException {
    int loopLen = 5;
    int maxRuntime = (loopLen + 1) * Math.max(1000, delayingListAppender.delay);

    // Thread.suspend/resume were removed in modern JDKs, so instead of
    // suspending the worker thread, block it inside the first append until
    // the gate opens, letting subsequent events pile up in the queue.
    final CountDownLatch gate = new CountDownLatch(1);
    final DelayingListAppender<Integer> la = new DelayingListAppender<Integer>() {
      @Override
      public void append(Integer e) {
        try {
          gate.await();
        } catch (InterruptedException ie) {
          interrupted = true;
        }
        super.append(e);
      }
    };
    la.setContext(context);
    la.setName("list");
    la.start();

    asyncAppenderBase.addAppender(la);
    asyncAppenderBase.setDiscardingThreshold(0);
    asyncAppenderBase.setMaxFlushTime(maxRuntime);
    asyncAppenderBase.start();

    // sentinel event: the worker dequeues it and blocks on the gate
    asyncAppenderBase.doAppend(-1);
    long deadline = System.currentTimeMillis() + maxRuntime;
    while (asyncAppenderBase.getNumberOfElementsInQueue() > 0 && System.currentTimeMillis() < deadline) {
      Thread.sleep(1);
    }

    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }
    assertEquals(loopLen, asyncAppenderBase.getNumberOfElementsInQueue());
    assertEquals(0, la.list.size());

    gate.countDown();
    asyncAppenderBase.stop();

    assertEquals(0, asyncAppenderBase.getNumberOfElementsInQueue());
    verify(la, loopLen + 1);
  }

  // @SuppressWarnings("deprecation")
  @Test
  public void stopExitsWhenMaxRuntimeReached() throws InterruptedException {
    int maxFlushTime = 1; // runtime of 0 means wait forever, so use 1 ms instead
    int loopLen = 10;
    ListAppender<Integer> la = delayingListAppender;
    asyncAppenderBase.addAppender(la);
    asyncAppenderBase.setMaxFlushTime(maxFlushTime);
    asyncAppenderBase.start();

    for (int i = 0; i < loopLen; i++) {
      asyncAppenderBase.doAppend(i);
    }

    asyncAppenderBase.stop();

    // confirms that stop exited when runtime reached
    statusChecker.assertContainsMatch("Max queue flush timeout \\(" + maxFlushTime + " ms\\) exceeded.");

    asyncAppenderBase.worker.join();

    // confirms that all entries do end up being flushed if we wait long enough
    verify(la, loopLen);
  }

  // Interruption of current thread when in doAppend method should not be consumed
  // by async appender. See also http://jira.qos.ch/browse/LOGBACK-910
  @Test
  public void verifyInterruptionIsNotSwallowed() {
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    Thread.currentThread().interrupt();
    asyncAppenderBase.doAppend(0);
    assertTrue(Thread.currentThread().isInterrupted());
    // clear interrupt flag for subsequent tests
    Thread.interrupted();
  }

  // Interruption of current thread should not prevent logging.
  // See also http://jira.qos.ch/browse/LOGBACK-910
  // and https://jira.qos.ch/browse/LOGBACK-1247
  @Test
  public void verifyInterruptionDoesNotPreventLogging() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.doAppend(0);
    Thread.currentThread().interrupt();
    asyncAppenderBase.doAppend(1);
    asyncAppenderBase.doAppend(1);
    assertTrue(Thread.currentThread().isInterrupted());
    asyncAppenderBase.stop();
    verify(listAppender, 3);
    // clear interrupt flag for subsequent tests
    Thread.interrupted();
  }

  @Test
  public void verifyInterruptionOfWorkerIsSwallowed() {
    asyncAppenderBase.addAppender(delayingListAppender);
    asyncAppenderBase.start();
    asyncAppenderBase.stop();
    assertFalse(asyncAppenderBase.worker.isAlive());
    assertFalse(asyncAppenderBase.worker.isInterrupted());
  }

  private void verify(ListAppender<Integer> la, int atLeast) {
    assertFalse(la.isStarted());
    assertTrue(atLeast+ " <= "+la.list.size(), atLeast <= la.list.size());
    statusChecker.assertIsErrorFree();
    statusChecker.assertContainsMatch("Worker thread will flush remaining events before exiting.");
  }

  static class LossyAsyncAppender extends AsyncAppenderBase<Integer> {
    @Override
    protected boolean isDiscardable(Integer i) {
      return (i % 3 == 0);
    }
  }

  @Test
  public void checkThatStartMethodIsIdempotent() {
    asyncAppenderBase.addAppender(lossyAsyncAppender);
    asyncAppenderBase.start();

    // we don't need mockito for this test, but if we did here is how it would look
    //AsyncAppenderBase<Integer>  spied = Mockito.spy(asyncAppenderBase);
    //Mockito.doThrow(new IllegalStateException("non idempotent start")).when((UnsynchronizedAppenderBase<Integer>) spied).start();

    // a second invocation of start will cause a IllegalThreadStateException thrown by the asyncAppenderBase.worker thread
    asyncAppenderBase.start();
  }

  @Test
  public void startWithoutAttachedAppenderReportsErrorAndDoesNotStart() {
    asyncAppenderBase.start();

    assertFalse(asyncAppenderBase.isStarted());
    assertEquals(Thread.State.NEW, asyncAppenderBase.worker.getState());
    statusChecker.assertContainsMatch(Status.ERROR, "No attached appenders found.");
  }

  @Test
  public void stopOfNonStartedAppenderDoesNothing() {
    asyncAppenderBase.addAppender(listAppender);
    int statusCountBeforeStop = context.getStatusManager().getCount();

    asyncAppenderBase.stop();

    assertFalse(asyncAppenderBase.isStarted());
    assertEquals(statusCountBeforeStop, context.getStatusManager().getCount());
    // the attached appender is only stopped by a worker that actually ran
    assertTrue(listAppender.isStarted());
  }

  @Test
  public void discardingThresholdDefaultsToOneFifthOfQueueSizeOnStart() {
    assertEquals(AsyncAppenderBase.UNDEFINED, asyncAppenderBase.getDiscardingThreshold());
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.setQueueSize(10);

    asyncAppenderBase.start();

    assertEquals(2, asyncAppenderBase.getDiscardingThreshold());
    statusChecker.assertContainsMatch("Setting discardingThreshold to 2");
    asyncAppenderBase.stop();
  }

  @Test
  public void explicitDiscardingThresholdIsKeptOnStart() {
    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.setQueueSize(10);
    asyncAppenderBase.setDiscardingThreshold(7);

    asyncAppenderBase.start();

    assertEquals(7, asyncAppenderBase.getDiscardingThreshold());
    statusChecker.assertContainsMatch("Setting discardingThreshold to 7");
    asyncAppenderBase.stop();
  }

  @Test
  public void maxFlushTimeDefaultsToOneSecondAndIsSettable() {
    assertEquals(AsyncAppenderBase.DEFAULT_MAX_FLUSH_TIME, asyncAppenderBase.getMaxFlushTime());
    asyncAppenderBase.setMaxFlushTime(123);
    assertEquals(123, asyncAppenderBase.getMaxFlushTime());
  }

  @Test
  public void neverBlockDefaultsToFalseAndIsSettable() {
    assertFalse(asyncAppenderBase.isNeverBlock());
    asyncAppenderBase.setNeverBlock(true);
    assertTrue(asyncAppenderBase.isNeverBlock());
  }

  @Test
  public void onlyTheFirstAppenderIsAttached() {
    ListAppender<Integer> second = new ListAppender<Integer>();
    second.setName("second");

    asyncAppenderBase.addAppender(listAppender);
    asyncAppenderBase.addAppender(second);

    statusChecker.assertContainsMatch(Status.INFO, "Attaching appender named \\[list\\] to AsyncAppender.");
    statusChecker.assertContainsMatch(Status.WARN, "One and only one appender may be attached to AsyncAppender.");
    statusChecker.assertContainsMatch(Status.WARN, "Ignoring additional appender named \\[second\\]");
    assertTrue(asyncAppenderBase.isAttached(listAppender));
    assertFalse(asyncAppenderBase.isAttached(second));
    assertSame(listAppender, asyncAppenderBase.getAppender("list"));
    assertNull(asyncAppenderBase.getAppender("second"));
    Iterator<Appender<Integer>> it = asyncAppenderBase.iteratorForAppenders();
    assertSame(listAppender, it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void detachAppenderByReferenceRemovesItWithoutStoppingIt() {
    asyncAppenderBase.addAppender(listAppender);

    assertTrue(asyncAppenderBase.detachAppender(listAppender));

    assertFalse(asyncAppenderBase.isAttached(listAppender));
    assertFalse(asyncAppenderBase.iteratorForAppenders().hasNext());
    assertTrue(listAppender.isStarted());
    assertFalse(asyncAppenderBase.detachAppender(listAppender));
  }

  @Test
  public void detachAppenderByNameRemovesItWithoutStoppingIt() {
    asyncAppenderBase.addAppender(listAppender);

    assertTrue(asyncAppenderBase.detachAppender("list"));

    assertNull(asyncAppenderBase.getAppender("list"));
    assertTrue(listAppender.isStarted());
    assertFalse(asyncAppenderBase.detachAppender("list"));
  }

  @Test
  public void detachAndStopAllAppendersStopsTheAttachedAppender() {
    asyncAppenderBase.addAppender(listAppender);

    asyncAppenderBase.detachAndStopAllAppenders();

    assertFalse(listAppender.isStarted());
    assertFalse(asyncAppenderBase.iteratorForAppenders().hasNext());
  }

  @Test(timeout = 5000)
  public void interruptedWorkerFlushesQueueAndExitsWhileAppenderIsStarted() throws InterruptedException {
    BlockingListAppender la = new BlockingListAppender(0);
    la.interruptWorkerOnRelease = true;
    la.setContext(context);
    la.setName("blocking");
    la.start();
    asyncAppenderBase.addAppender(la);
    asyncAppenderBase.setQueueSize(10);
    asyncAppenderBase.start();
    assertEquals(10, asyncAppenderBase.getRemainingCapacity());

    asyncAppenderBase.doAppend(0);
    la.entered.await();
    // the worker is now blocked inside append(0): these stay in the queue
    asyncAppenderBase.doAppend(1);
    asyncAppenderBase.doAppend(2);
    assertEquals(8, asyncAppenderBase.getRemainingCapacity());

    // on release, the worker interrupts itself, so its next take() throws
    // InterruptedException although the async appender is still started
    la.release.countDown();
    asyncAppenderBase.worker.join();

    assertTrue(asyncAppenderBase.isStarted());
    assertEquals(Arrays.asList(0, 1, 2), la.list);
    assertEquals(0, asyncAppenderBase.getNumberOfElementsInQueue());
    assertFalse("worker should stop the attached appender on exit", la.isStarted());
    assertFalse(asyncAppenderBase.worker.isInterrupted());
    statusChecker.assertContainsMatch("Worker thread will flush remaining events before exiting.");

    asyncAppenderBase.stop();
    assertFalse(asyncAppenderBase.isStarted());
    statusChecker.assertContainsMatch("Queue flush finished successfully within timeout.");
    statusChecker.assertIsErrorFree();
  }

  @Test(timeout = 5000)
  public void stopReportsErrorWhenInterruptedWhileWaitingForWorker() throws InterruptedException {
    final BlockingListAppender la = new BlockingListAppender(0);
    la.setContext(context);
    la.setName("blocking");
    la.start();
    asyncAppenderBase.addAppender(la);
    // 0 = wait for the worker without a time limit
    asyncAppenderBase.setMaxFlushTime(0);
    asyncAppenderBase.start();

    asyncAppenderBase.doAppend(0);
    la.entered.await();
    asyncAppenderBase.doAppend(1);
    asyncAppenderBase.doAppend(2);

    Thread stopper = new Thread(new Runnable() {
      @Override
      public void run() {
        asyncAppenderBase.stop();
      }
    }, "AsyncAppenderBaseTest-stopper");
    stopper.start();
    // The worker ignores interrupts until released, so stop() can only return
    // by being interrupted while it waits for the worker to finish.
    while (stopper.isAlive()) {
      stopper.interrupt();
      stopper.join(10);
    }

    assertFalse(asyncAppenderBase.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR,
        "Failed to join worker thread. 2 queued events may be discarded.");
    statusChecker.asssertContainsException(InterruptedException.class);
    statusChecker.assertNoMatch("Queue flush finished successfully");

    // let the worker finish so that it doesn't outlive this test
    la.release.countDown();
    asyncAppenderBase.worker.join();
    assertEquals(Arrays.asList(0, 1, 2), la.list);
  }

  /**
   * A ListAppender that blocks the calling (worker) thread inside the append
   * of {@link #blockingEvent} until {@link #release} opens, ignoring interrupts
   * meanwhile. It restores a swallowed interrupt on release, and interrupts its
   * thread on release anyway if {@link #interruptWorkerOnRelease} is set.
   */
  static class BlockingListAppender extends ListAppender<Integer> {
    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);
    final int blockingEvent;
    volatile boolean interruptWorkerOnRelease;

    BlockingListAppender(int blockingEvent) {
      this.blockingEvent = blockingEvent;
    }

    @Override
    protected void append(Integer e) {
      super.append(e);
      if (e != blockingEvent) {
        return;
      }
      entered.countDown();
      boolean interrupted = false;
      while (true) {
        try {
          release.await();
          break;
        } catch (InterruptedException ie) {
          interrupted = true;
        }
      }
      if (interrupted || interruptWorkerOnRelease) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
