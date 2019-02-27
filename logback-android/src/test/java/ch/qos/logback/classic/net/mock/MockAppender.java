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
package ch.qos.logback.classic.net.mock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * A mock {@link AppenderBase} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
public class MockAppender extends AppenderBase<ILoggingEvent> {

  private final Lock lock = new ReentrantLock();
  private final Condition appendCondition = lock.newCondition();
  private final BlockingQueue<ILoggingEvent> events =
      new LinkedBlockingQueue<ILoggingEvent>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    lock.lock();
    try {
      events.offer(eventObject);
      appendCondition.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  public ILoggingEvent awaitAppend(long delay) throws InterruptedException {
    return events.poll(delay, TimeUnit.MILLISECONDS);
  }

  public ILoggingEvent getLastEvent() {
    return events.peek();
  }

}
