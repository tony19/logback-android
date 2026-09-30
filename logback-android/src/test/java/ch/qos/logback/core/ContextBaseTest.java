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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.StatusManager;

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

  @Test
  public void setStatusManagerRejectsNull() {
    StatusManager original = context.getStatusManager();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
      @Override
      public void run() {
        context.setStatusManager(null);
      }
    });

    assertEquals("null StatusManager not allowed", e.getMessage());
    assertSame(original, context.getStatusManager());
  }

  @Test
  public void setStatusManagerReplacesStatusManager() {
    StatusManager replacement = new BasicStatusManager();
    context.setStatusManager(replacement);
    assertSame(replacement, context.getStatusManager());
  }

  @Test
  public void nullNameIsAcceptedByUnnamedContext() {
    context.setName(null);
    assertNull(context.getName());
    context.setName("hello");
    assertEquals("hello", context.getName());
  }

  @Test
  public void nullNameCannotReplaceExistingName() {
    context.setName("hello");

    assertThrows(IllegalStateException.class, new ThrowingRunnable() {
      @Override
      public void run() {
        context.setName(null);
      }
    });

    assertEquals("hello", context.getName());
  }

  @Test
  public void isStartedFollowsStartAndStop() {
    assertFalse(context.isStarted());
    context.start();
    assertTrue(context.isStarted());
    context.stop();
    assertFalse(context.isStarted());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void getExecutorServiceReturnsTheScheduledExecutorService() {
    try {
      ExecutorService executorService = context.getExecutorService();
      assertSame(context.getScheduledExecutorService(), executorService);
    } finally {
      context.stop();
    }
  }

  @Test
  public void resetRemovesRegisteredShutdownHook() {
    Thread hook = new Thread();
    Runtime.getRuntime().addShutdownHook(hook);
    boolean hookStillRegistered = true;
    try {
      context.putObject(CoreConstants.SHUTDOWN_HOOK_THREAD, hook);

      context.reset();

      assertNull(context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD));
      // false: reset() already unregistered it
      hookStillRegistered = Runtime.getRuntime().removeShutdownHook(hook);
      assertFalse(hookStillRegistered);
    } finally {
      if (hookStillRegistered) {
        Runtime.getRuntime().removeShutdownHook(hook);
      }
    }
  }

  @Test
  public void resetToleratesShutdownInProgress() {
    Thread hook = new Thread();
    Runtime runtime = mock(Runtime.class);
    doThrow(new IllegalStateException("Shutdown in progress")).when(runtime).removeShutdownHook(hook);
    context.putObject(CoreConstants.SHUTDOWN_HOOK_THREAD, hook);
    context.putProperty("keyA", "valA");

    try (MockedStatic<Runtime> runtimeClass = mockStatic(Runtime.class)) {
      runtimeClass.when(new MockedStatic.Verification() {
        @Override
        public void apply() {
          Runtime.getRuntime();
        }
      }).thenReturn(runtime);

      context.reset();
    }

    verify(runtime).removeShutdownHook(hook);
    assertNull(context.getObject(CoreConstants.SHUTDOWN_HOOK_THREAD));
    assertNull(context.getProperty("keyA"));
    assertTrue(lifeCycleManager.isReset());
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
