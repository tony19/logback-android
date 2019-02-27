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

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;

/**
 * A {@link ServerRunner} that listens for connections from remote receiver
 * component clients and delivers logging events to all connected clients.
 *
 * @author Carl Harris
 */
class RemoteReceiverServerRunner
    extends ConcurrentServerRunner<RemoteReceiverClient> {

  private final int clientQueueSize;

  /**
   * Constructs a new server runner.
   * @param listener the listener from which the server will accept new
   *    clients
   * @param executor that will be used to execute asynchronous tasks
   *    on behalf of the runner.
   * @param queueSize size of the event queue that will be maintained for
   *    each client
   */
  public RemoteReceiverServerRunner(
      ServerListener<RemoteReceiverClient> listener, Executor executor,
      int clientQueueSize) {
    super(listener, executor);
    this.clientQueueSize = clientQueueSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean configureClient(RemoteReceiverClient client) {
    client.setContext(getContext());
    client.setQueue(new ArrayBlockingQueue<Serializable>(clientQueueSize));
    return true;
  }

}
