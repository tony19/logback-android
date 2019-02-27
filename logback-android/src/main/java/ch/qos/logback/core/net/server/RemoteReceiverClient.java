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
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.core.spi.ContextAware;


/**
 * A client of a {@link ServerRunner} that receives events from a local
 * appender and logs them according to local policy.
 *
 * @author Carl Harris
 */
interface RemoteReceiverClient extends Client, ContextAware {

  /**
   * Sets the client's event queue.
   * <p>
   * This method must be invoked before the {@link #run()} method is invoked.
   * @param queue the queue to set
   */
  void setQueue(BlockingQueue<Serializable> queue);

  /**
   * Offers an event to the client.
   * @param event the subject event
   * @return {@code true} if the client's queue accepted the event,
   *    {@code false} if the client's queue is full
   */
  boolean offer(Serializable event);

}
