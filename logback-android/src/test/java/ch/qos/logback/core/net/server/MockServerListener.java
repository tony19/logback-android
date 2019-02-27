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

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.core.net.server.Client;
import ch.qos.logback.core.net.server.ServerListener;


/**
 * A mock {@link ServerListener} that has a blocking queue to pass a client
 * to a {@link #acceptClient()} caller.  If the {@link #close()} method is
 * called while a caller is blocked waiting to take from the queue, the
 * caller's thread is interrupted.
 *
 * @author Carl Harris
 */
public class MockServerListener<T extends Client> implements ServerListener<T> {

  private final BlockingQueue<T> queue =
      new LinkedBlockingQueue<T>();

  private boolean closed;
  private Thread waiter;

  public synchronized Thread getWaiter() {
    return waiter;
  }

  public synchronized void setWaiter(Thread waiter) {
    this.waiter = waiter;
  }

  public synchronized boolean isClosed() {
    return closed;
  }

  public synchronized void setClosed(boolean closed) {
    this.closed = closed;
  }

  public T acceptClient() throws IOException, InterruptedException {
    if (isClosed()) {
      throw new IOException("closed");
    }
    setWaiter(Thread.currentThread());
    try {
      return queue.take();
    }
    finally {
      setWaiter(null);
    }
  }

  public void addClient(T client) {
    queue.offer(client);
  }

  public synchronized void close() {
    setClosed(true);
    Thread waiter = getWaiter();
    if (waiter != null) {
      waiter.interrupt();
    }
  }

}
