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

import ch.qos.logback.core.spi.ContextAware;

/**
 * An object that is responsible for the asynchronous execution of a
 * socket server.
 * <p>
 * This interface exists primarily to allow the runner to be mocked for
 * the purpose of unit testing the socket server implementation.
 *
 * @author Carl Harris
 */
public interface ServerRunner<T extends Client> extends ContextAware, Runnable {

  /**
   * Gets a flag indicating whether the server is currently running.
   * @return flag state
   */
  boolean isRunning();

  /**
   * Stops execution of the runner.
   * <p>
   * This method must cause all I/O and thread resources associated with
   * the runner to be released.  If the receiver has not been started, this
   * method must have no effect.
   * @throws IOException failed to stop runner
   */
  void stop() throws IOException;

  /**
   * Presents each connected client to the given visitor.
   * @param visitor the subject visitor
   */
  void accept(ClientVisitor<T> visitor);

}
