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
package ch.qos.logback.core.net.mock;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.server.MockScheduledExecutorService;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;

/**
 * A mock {@link Context} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
public class MockContext extends ContextBase {

  private final ScheduledExecutorService scheduledExecutorService;

  private Status lastStatus;

  public MockContext() {
    this(new MockScheduledExecutorService());
  }

  public MockContext(ScheduledExecutorService executorService) {
    this.setStatusManager(new MockStatusManager());
    this.scheduledExecutorService = executorService;
  }

  @Override
  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  public Status getLastStatus() {
    return lastStatus;
  }

  public void setLastStatus(Status lastStatus) {
    this.lastStatus = lastStatus;
  }

  private class MockStatusManager implements StatusManager {

    public void add(Status status) {
      lastStatus = status;
    }

    public List<Status> getCopyOfStatusList() {
      throw new UnsupportedOperationException();
    }

    public int getCount() {
      throw new UnsupportedOperationException();
    }

    public boolean add(StatusListener listener) {
      throw new UnsupportedOperationException();
    }

    public boolean addUniquely(StatusListener listener, Object obj) {
      throw new UnsupportedOperationException();
    }

    public void remove(StatusListener listener) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      throw new UnsupportedOperationException();
    }

    public List<StatusListener> getCopyOfStatusListenerList() {
      throw new UnsupportedOperationException();
    }

  }

}
