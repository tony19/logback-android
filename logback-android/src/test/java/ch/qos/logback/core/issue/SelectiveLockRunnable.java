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
package ch.qos.logback.core.issue;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.core.contention.RunnableWithCounterAndDone;

/**
 * A runnable which behaves differently depending on the desired locking model.
 * 
 * @author Joern Huxhorn
 * @author Ceki Gulcu
 */
public class SelectiveLockRunnable extends RunnableWithCounterAndDone {

  enum LockingModel {
    NOLOCK, SYNC, FAIR, UNFAIR;
  }

  static Object LOCK = new Object();
  static Lock FAIR_LOCK = new ReentrantLock(true);
  static Lock UNFAIR_LOCK = new ReentrantLock(false);

  LockingModel model;

  SelectiveLockRunnable(LockingModel model) {
    this.model = model;
  }

  public void run() {
    switch (model) {
    case NOLOCK:
      nolockRun();
      break;
    case SYNC:
      synchronizedRun();
      break;
    case FAIR:
      fairLockRun();
      break;
    case UNFAIR:
      unfairLockRun();
      break;
    }
  }

  void fairLockRun() {
    for (;;) {
      FAIR_LOCK.lock();
      counter++;
      FAIR_LOCK.unlock();
      if (done) {
        return;
      }
    }
  }

  void unfairLockRun() {
    for (;;) {
      UNFAIR_LOCK.lock();
      counter++;
      UNFAIR_LOCK.unlock();
      if (done) {
        return;
      }
    }
  }

  void nolockRun() {
    for (;;) {
      counter++;
      if (done) {
        return;
      }
    }
  }

  void synchronizedRun() {
    for (;;) {
      synchronized (LOCK) {
        counter++;
      }
      if (done) {
        return;
      }
    }
  }
  
  @Override
  public String toString() {
    return "SelectiveLockRunnable "+model;
  }
}
