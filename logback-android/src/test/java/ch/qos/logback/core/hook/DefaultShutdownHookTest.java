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
package ch.qos.logback.core.hook;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultShutdownHookTest {

  private final ContextBase context = new ContextBase();
  private final StatusChecker checker = new StatusChecker(context);
  private final DefaultShutdownHook hook = new DefaultShutdownHook();

  @Before
  public void setUp() {
    hook.setContext(context);
    context.start();
    assertTrue(context.isStarted());
  }

  @After
  public void tearDown() {
    // never leak an interrupt flag into other tests
    Thread.interrupted();
    context.stop();
  }

  @Test
  public void delayDefaultsToZero() {
    assertSame(DefaultShutdownHook.DEFAULT_DELAY, hook.getDelay());
    assertEquals(0, hook.getDelay().getMilliseconds());
  }

  @Test
  public void setDelayIsReturnedByGetter() {
    Duration delay = Duration.buildBySeconds(3);

    hook.setDelay(delay);

    assertSame(delay, hook.getDelay());
  }

  @Test
  public void runWithoutDelayStopsContextWithoutSleeping() {
    hook.run();

    assertFalse(context.isStarted());
    checker.assertContainsMatch(Status.INFO, "Logback context being closed via shutdown hook");
    checker.assertNoMatch("Sleeping for");
  }

  @Test
  public void runWithDelayReportsSleepThenStopsContext() {
    hook.setDelay(Duration.buildByMilliseconds(1));

    hook.run();

    assertFalse(context.isStarted());
    checker.assertContainsMatch(Status.INFO, "Sleeping for 1 milliseconds");
    checker.assertContainsMatch(Status.INFO, "Logback context being closed via shutdown hook");
  }

  @Test
  public void runStillStopsContextWhenInterruptedWhileSleeping() {
    // a delay long enough that the test could only finish because the sleep
    // was cut short by the pending interrupt
    hook.setDelay(Duration.buildByHours(1));
    Thread.currentThread().interrupt();

    hook.run();

    assertFalse(context.isStarted());
    checker.assertContainsMatch(Status.INFO, "Sleeping for 1 hours");
    checker.assertContainsMatch(Status.INFO, "Logback context being closed via shutdown hook");
    // the InterruptedException is swallowed; sleep() cleared the interrupt flag
    assertFalse(Thread.currentThread().isInterrupted());
  }
}
