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
package ch.qos.logback.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;

/**
 * Unit tests for {@link InterruptUtil}.
 */
public class InterruptUtilTest {

  Context context = new ContextBase();

  @After
  public void tearDown() {
    // never leak an interrupt into the next test
    Thread.interrupted();
  }

  @Test
  public void maskAndUnmaskOfPreviouslyInterruptedThread() {
    Thread.currentThread().interrupt();
    InterruptUtil interruptUtil = new InterruptUtil(context);

    interruptUtil.maskInterruptFlag();
    assertFalse(Thread.currentThread().isInterrupted());

    interruptUtil.unmaskInterruptFlag();
    assertTrue(Thread.currentThread().isInterrupted());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void maskAndUnmaskOfNonInterruptedThreadDoNothing() {
    InterruptUtil interruptUtil = new InterruptUtil(context);

    // an interrupt arriving after construction isn't masked
    Thread.currentThread().interrupt();
    interruptUtil.maskInterruptFlag();
    assertTrue(Thread.interrupted());

    // and none is restored
    interruptUtil.unmaskInterruptFlag();
    assertFalse(Thread.currentThread().isInterrupted());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void unmaskReportsFailureToReinterrupt() throws InterruptedException {
    DeniedSelfInterruptThread thread = new DeniedSelfInterruptThread(context);
    thread.start();
    thread.join();

    assertNull(thread.failure);
    assertFalse("interrupt flag should not have been restored", thread.interruptedAfterUnmask);
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals("Failed to intrreupt current thread", status.getMessage());
    assertSame(thread.denial, status.getThrowable());
  }

  /**
   * A thread that interrupts itself before using {@link InterruptUtil}, and
   * then refuses to be interrupted with a {@link SecurityException}.
   */
  private static class DeniedSelfInterruptThread extends Thread {
    private final Context context;
    final SecurityException denial = new SecurityException("interrupt denied");
    private volatile boolean denyInterrupts;
    volatile boolean interruptedAfterUnmask;
    volatile Throwable failure;

    DeniedSelfInterruptThread(Context context) {
      this.context = context;
    }

    @Override
    public void interrupt() {
      if (denyInterrupts) {
        throw denial;
      }
      super.interrupt();
    }

    @Override
    public void run() {
      try {
        interrupt();
        InterruptUtil interruptUtil = new InterruptUtil(context);
        interruptUtil.maskInterruptFlag();
        denyInterrupts = true;
        interruptUtil.unmaskInterruptFlag();
        interruptedAfterUnmask = isInterrupted();
      } catch (Throwable t) {
        failure = t;
      }
    }
  }
}
