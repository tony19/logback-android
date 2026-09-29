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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.MockScheduledExecutorService;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;

/**
 * Unit tests for {@link ReceiverBase}.
 */
public class ReceiverBaseTest {

  private final MockScheduledExecutorService executor = new MockScheduledExecutorService();
  private final MockContext context = new MockContext(executor);
  private final TestReceiver receiver = new TestReceiver();

  @Test
  public void startWithoutContextFails() {
    IllegalStateException ex = assertThrows(IllegalStateException.class, new ThrowingRunnable() {
      @Override
      public void run() {
        receiver.start();
      }
    });

    assertEquals("context not set", ex.getMessage());
    assertFalse(receiver.isStarted());
  }

  @Test
  public void startRunsTheTaskOnlyWhenTheReceiverShouldStart() {
    receiver.setContext(context);
    receiver.shouldStart = false;
    receiver.start();
    assertFalse(receiver.isStarted());
    assertEquals(0, receiver.taskRuns);

    receiver.shouldStart = true;
    receiver.start();
    assertTrue(receiver.isStarted());
    assertSame(receiver.task, executor.getLastCommand());
    assertEquals(1, receiver.taskRuns);

    // starting again is a no-op
    receiver.start();
    assertEquals(1, receiver.taskRuns);
  }

  @Test
  public void stopReportsFailureOfOnStopAndStillStops() {
    receiver.setContext(context);
    receiver.start();
    RuntimeException failure = new IllegalStateException("cannot stop");
    receiver.onStopFailure = failure;

    receiver.stop();

    assertFalse(receiver.isStarted());
    Status status = context.getLastStatus();
    assertTrue(status instanceof ErrorStatus);
    assertEquals("on stop: " + failure, status.getMessage());
    assertSame(failure, status.getThrowable());
  }

  @Test
  public void stopWhenNotStartedDoesNotCallOnStop() {
    receiver.setContext(context);
    receiver.stop();
    assertEquals(0, receiver.onStopCount);
  }

  private static class TestReceiver extends ReceiverBase {
    int taskRuns;
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        taskRuns++;
      }
    };
    boolean shouldStart = true;
    RuntimeException onStopFailure;
    int onStopCount;

    @Override
    protected boolean shouldStart() {
      return shouldStart;
    }

    @Override
    protected void onStop() {
      onStopCount++;
      if (onStopFailure != null) {
        throw onStopFailure;
      }
    }

    @Override
    protected Runnable getRunnableTask() {
      return task;
    }
  }
}
