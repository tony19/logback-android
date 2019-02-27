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
package ch.qos.logback.core.net.server;

import ch.qos.logback.core.net.server.Client;



/**
 *
 * A mock {@link Client} that notifies waiting thread when it has started,
 * and waits to be interrupted before exiting.
 *
 * @author Carl Harris
 */
class MockClient implements Client {

  private boolean running;
  private boolean closed;

  public void run() {
    synchronized (this) {
      running = true;
      notifyAll();
      while (running && !Thread.currentThread().isInterrupted()) {
        try {
          wait();
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public void close() {
    synchronized (this) {
      running = false;
      closed = true;
      notifyAll();
    }
  }

  public synchronized boolean isRunning() {
    return running;
  }

  public synchronized boolean isClosed() {
    return closed;
  }

}
