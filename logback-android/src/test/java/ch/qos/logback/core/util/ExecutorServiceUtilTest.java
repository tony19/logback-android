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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import ch.qos.logback.core.CoreConstants;

/**
 * Unit tests for {@link ExecutorServiceUtil}.
 */
public class ExecutorServiceUtilTest {

  private static final Callable<Thread> CURRENT_THREAD = new Callable<Thread>() {
    @Override
    public Thread call() {
      return Thread.currentThread();
    }
  };

  private ExecutorService executorService;

  @After
  public void tearDown() throws InterruptedException {
    if (executorService != null) {
      executorService.shutdownNow();
      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @Test
  public void isInstantiable() {
    // the class only has static members, but its implicit constructor is public
    assertNotNull(new ExecutorServiceUtil());
  }

  @Test
  public void newExecutorServiceIsConfiguredForLogbackComponents() throws Exception {
    executorService = ExecutorServiceUtil.newExecutorService();

    ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
    assertEquals(CoreConstants.CORE_POOL_SIZE, tpe.getCorePoolSize());
    assertEquals(CoreConstants.MAX_POOL_SIZE, tpe.getMaximumPoolSize());
    assertEquals(0L, tpe.getKeepAliveTime(TimeUnit.MILLISECONDS));
    assertTrue(tpe.getQueue() instanceof SynchronousQueue);
    assertSame(logbackThreadFactory(), tpe.getThreadFactory());

    Thread worker = executorService.submit(CURRENT_THREAD).get(10, TimeUnit.SECONDS);
    assertTrue(worker.isDaemon());
    assertTrue(worker.getName(), worker.getName().matches("logback-\\d+"));
  }

  @Test
  public void newScheduledExecutorServiceIsConfiguredForLogbackComponents() throws Exception {
    ScheduledExecutorService ses = ExecutorServiceUtil.newScheduledExecutorService();
    executorService = ses;

    ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) ses;
    assertEquals(CoreConstants.SCHEDULED_EXECUTOR_POOL_SIZE, stpe.getCorePoolSize());
    assertSame(logbackThreadFactory(), stpe.getThreadFactory());

    Thread worker = ses.schedule(CURRENT_THREAD, 0, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
    assertTrue(worker.isDaemon());
    assertTrue(worker.getName(), worker.getName().matches("logback-\\d+"));
  }

  @Test
  public void shutdownInterruptsRunningTasks() throws Exception {
    executorService = ExecutorServiceUtil.newExecutorService();
    final CountDownLatch started = new CountDownLatch(1);
    final CountDownLatch never = new CountDownLatch(1);
    final AtomicBoolean interrupted = new AtomicBoolean(false);
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        started.countDown();
        try {
          never.await();
        } catch (InterruptedException e) {
          interrupted.set(true);
        }
      }
    });
    started.await();

    ExecutorServiceUtil.shutdown(executorService);

    assertTrue(executorService.isShutdown());
    assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    assertTrue(interrupted.get());
  }

  @Test
  public void threadFactoryMakesDaemonThreadsNamedSequentially() throws Exception {
    ThreadFactory factory = newLogbackThreadFactory(null);
    Runnable noop = new Runnable() {
      @Override
      public void run() {
      }
    };

    Thread first = factory.newThread(noop);
    Thread second = factory.newThread(noop);

    assertTrue(first.isDaemon());
    assertEquals("logback-1", first.getName());
    assertTrue(second.isDaemon());
    assertEquals("logback-2", second.getName());
  }

  @Test
  public void threadFactoryLeavesTheDaemonFlagOfDaemonThreadsUntouched() throws Exception {
    final CountDownLatch release = new CountDownLatch(1);
    // Thread.setDaemon() throws for a live thread, so the factory must not
    // call it for a thread that already is a daemon
    final Thread liveDaemon = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          release.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    liveDaemon.setDaemon(true);
    liveDaemon.start();
    try {
      ThreadFactory factory = newLogbackThreadFactory(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return liveDaemon;
        }
      });

      Thread thread = factory.newThread(null);

      assertSame(liveDaemon, thread);
      assertTrue(thread.isDaemon());
      assertEquals("logback-1", thread.getName());
    } finally {
      release.countDown();
      liveDaemon.join();
    }
    assertFalse(liveDaemon.isAlive());
  }

  private static ThreadFactory logbackThreadFactory() throws Exception {
    Field field = ExecutorServiceUtil.class.getDeclaredField("THREAD_FACTORY");
    field.setAccessible(true);
    return (ThreadFactory) field.get(null);
  }

  /**
   * Creates another instance of the logback thread factory, with its own
   * thread counter and, if {@code delegate} isn't null, with {@code delegate}
   * standing in for the JDK's default thread factory.
   */
  private static ThreadFactory newLogbackThreadFactory(ThreadFactory delegate) throws Exception {
    Class<?> factoryClass = logbackThreadFactory().getClass();
    Constructor<?> constructor = factoryClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    ThreadFactory factory = (ThreadFactory) constructor.newInstance();
    if (delegate != null) {
      Field defaultFactory = factoryClass.getDeclaredField("defaultFactory");
      defaultFactory.setAccessible(true);
      defaultFactory.set(factory, delegate);
    }
    return factory;
  }
}
