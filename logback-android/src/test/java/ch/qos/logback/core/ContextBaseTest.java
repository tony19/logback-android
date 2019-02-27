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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import ch.qos.logback.core.spi.LifeCycle;

public class ContextBaseTest {

  private InstrumentedLifeCycleManager lifeCycleManager =
      new InstrumentedLifeCycleManager();

  private InstrumentedContextBase context =
      new InstrumentedContextBase(lifeCycleManager);

  @Test
  public void renameDefault() {
    context.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
    context.setName("hello");
  }


  @Test
  public void idempotentNameTest() {
    context.setName("hello");
    context.setName("hello");
  }

  @Test
  public void renameTest() {
    context.setName("hello");
    try {
      context.setName("x");
      fail("renaming is not allowed");
    } catch (IllegalStateException ise) {
    }
  }

  @Test
  public void resetTest() {
    context.setName("hello");
    context.putProperty("keyA", "valA");
    context.putObject("keyA", "valA");
    assertEquals("valA", context.getProperty("keyA"));
    assertEquals("valA", context.getObject("keyA"));
    MockLifeCycleComponent component = new MockLifeCycleComponent();
    context.register(component);
    assertSame(component, lifeCycleManager.getLastComponent());
    context.reset();
    assertNull(context.getProperty("keyA"));
    assertNull(context.getObject("keyA"));
    assertTrue(lifeCycleManager.isReset());
  }

  @Test
  public void contextNameProperty() {
    assertNull(context.getProperty(CoreConstants.CONTEXT_NAME_KEY));
    String HELLO = "hello";
    context.setName(HELLO);
    assertEquals(HELLO, context.getProperty(CoreConstants.CONTEXT_NAME_KEY));
    // good to have a raw reference to the "CONTEXT_NAME" as most clients would
    // not go through CoreConstants
    assertEquals(HELLO, context.getProperty("CONTEXT_NAME"));
  }

  @Test
  public void contextThreadpoolIsDaemonized() throws InterruptedException {
    ExecutorService execSvc = context.getScheduledExecutorService();
    final ArrayList<Thread> executingThreads = new ArrayList<Thread>();
    execSvc.execute(new Runnable() {
      @Override
      public void run() {
        synchronized (executingThreads) {
          executingThreads.add(Thread.currentThread());
          executingThreads.notifyAll();
        }
      }
    });
    synchronized (executingThreads) {
      while (executingThreads.isEmpty()) {
        executingThreads.wait();
      }
    }
    assertTrue("executing thread should be a daemon thread.", executingThreads.get(0).isDaemon());
  }

  private static class InstrumentedContextBase extends ContextBase {

    private final LifeCycleManager lifeCycleManager;

    public InstrumentedContextBase(LifeCycleManager lifeCycleManager) {
      this.lifeCycleManager = lifeCycleManager;
    }

    @Override
    protected LifeCycleManager getLifeCycleManager() {
      return lifeCycleManager;
    }

  }

  private static class InstrumentedLifeCycleManager extends LifeCycleManager {

    private LifeCycle lastComponent;
    private boolean reset;

    @Override
    public void register(LifeCycle component) {
      lastComponent = component;
      super.register(component);
    }

    @Override
    public void reset() {
      reset = true;
      super.reset();
    }

    public LifeCycle getLastComponent() {
      return lastComponent;
    }

    public boolean isReset() {
      return reset;
    }

  }

}
