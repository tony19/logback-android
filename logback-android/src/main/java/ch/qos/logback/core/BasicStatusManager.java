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

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.spi.LogbackLock;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;

public class BasicStatusManager implements StatusManager {

  public static final int MAX_HEADER_COUNT = 150;
  public static final int TAIL_SIZE = 150;

  int count = 0;

  // protected access was requested in http://jira.qos.ch/browse/LBCORE-36
  final protected List<Status> statusList = new ArrayList<Status>();
  final protected CyclicBuffer<Status> tailBuffer = new CyclicBuffer<Status>(
      TAIL_SIZE);
  final protected LogbackLock statusListLock = new LogbackLock();

  int level = Status.INFO;

  // protected access was requested in http://jira.qos.ch/browse/LBCORE-36
  final protected List<StatusListener> statusListenerList = new ArrayList<StatusListener>();
  final protected LogbackLock statusListenerListLock = new LogbackLock();

  // Note on synchronization
  // This class contains two separate locks statusListLock and
  // statusListenerListLock guarding respectively the statusList+tailBuffer and
  // statusListenerList fields. The locks are used internally
  // without cycles. They are exposed to derived classes which should be careful
  // not to create deadlock cycles.

  /**
   * Add a new status object.
   *
   * @param newStatus
   *                the status message to add
   */
  public void add(Status newStatus) {
    // LBCORE-72: fire event before the count check
    fireStatusAddEvent(newStatus);

    count++;
    if (newStatus.getLevel() > level) {
      level = newStatus.getLevel();
    }

    synchronized (statusListLock) {
      if (statusList.size() < MAX_HEADER_COUNT) {
        statusList.add(newStatus);
      } else {
        tailBuffer.add(newStatus);
      }
    }

  }

  public List<Status> getCopyOfStatusList() {
    synchronized (statusListLock) {
      List<Status> tList = new ArrayList<Status>(statusList);
      tList.addAll(tailBuffer.asList());
      return tList;
    }
  }

  private void fireStatusAddEvent(Status status) {
    synchronized (statusListenerListLock) {
      for (StatusListener sl : statusListenerList) {
        sl.addStatusEvent(status);
      }
    }
  }

  public void clear() {
    synchronized (statusListLock) {
      count = 0;
      statusList.clear();
      tailBuffer.clear();
    }
  }

  public int getLevel() {
    return level;
  }

  public int getCount() {
    return count;
  }

  /**
   * This implementation does not allow duplicate installations of OnConsoleStatusListener
   * @param listener
   */
  public boolean add(StatusListener listener) {
    synchronized (statusListenerListLock) {
      if (listener instanceof OnConsoleStatusListener) {
        boolean alreadyPresent = checkForPresence(statusListenerList, listener.getClass());
        if (alreadyPresent) {
          return false;
        }
      }
      statusListenerList.add(listener);
    }
    return true;
  }

  private boolean checkForPresence(List<StatusListener> statusListenerList, Class<?> aClass) {
    for (StatusListener e: statusListenerList) {
      if (e.getClass() == aClass) {
        return true;
      }
    }
    return false;
  }

  public boolean addUniquely(StatusListener newListener, Object origin) {
    for (StatusListener listener : getCopyOfStatusListenerList()) {
      if (listener.getClass().isInstance(newListener)) {
        add(new WarnStatus("A previous listener of type [" + listener.getClass()
                + "] has been already registered. Skipping double registration.", origin));
        return false;
      }
    }
    add(newListener);
    return true;
  }

  public void remove(StatusListener listener) {
    synchronized (statusListenerListLock) {
      statusListenerList.remove(listener);
    }
  }

  public List<StatusListener> getCopyOfStatusListenerList() {
    synchronized (statusListenerListLock) {
      return new ArrayList<StatusListener>(statusListenerList);
    }
  }

}
