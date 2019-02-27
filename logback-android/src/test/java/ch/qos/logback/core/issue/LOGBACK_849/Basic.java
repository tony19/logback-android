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
package ch.qos.logback.core.issue.LOGBACK_849;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.util.ExecutorServiceUtil;


public class Basic {

  ExecutorService executor = ExecutorServiceUtil.newScheduledExecutorService();
  Context context = new ContextBase();

  @Test(timeout = 100)
  public void withNoSubmittedTasksShutdownNowShouldReturnImmediately() throws InterruptedException {
    executor.shutdownNow();
    executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
  }

  @Ignore
  @Test
  public void withOneSlowTask() throws InterruptedException {
    executor.execute(new InterruptIgnoring(1000));
    Thread.sleep(100);
    ExecutorServiceUtil.shutdown(executor);
  }

  //  InterruptIgnoring ===========================================
  static class InterruptIgnoring implements Runnable {

    int delay;

    InterruptIgnoring(int delay) {
      this.delay = delay;
    }

    public void run() {
      long runUntil = System.currentTimeMillis() + delay;

      while (true) {
        try {
          long sleep = runUntil - System.currentTimeMillis();
          System.out.println("will sleep " + sleep);
          if (sleep > 0) {
            Thread.sleep(delay);
          } else {
            return;
          }
        } catch (InterruptedException e) {
          // ignore the exception
        }
      }
    }
  }


}
