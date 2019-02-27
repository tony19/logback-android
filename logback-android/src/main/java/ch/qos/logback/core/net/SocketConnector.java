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
package ch.qos.logback.core.net;

import java.net.Socket;
import java.util.concurrent.Callable;

import javax.net.SocketFactory;

/**
 * A {@link Runnable} that (re)connects a socket.
 * <p>
 * An implementation of this interface is responsible for repeatedly
 * attempting to create a socket connection to a remote host.
 *
 * @author Carl Harris
 */
public interface SocketConnector extends Callable<Socket> {

  /**
   * An exception handler that is notified of all exceptions that occur
   * during the (re)connection process.
   */
  public interface ExceptionHandler {
    void connectionFailed(SocketConnector connector, Exception ex);
  }

  /**
   * Blocks the calling thread until a connection is successfully
   * established.
   * @return the connected socket
   * @throws InterruptedException the running connection thread was cancelled
   */
  Socket call() throws InterruptedException;

  /**
   * Sets the connector's exception handler.
   * <p>
   * The handler must be set before the {@link #call()} method is invoked.
   * @param exceptionHandler the handler to set
   */
  void setExceptionHandler(ExceptionHandler exceptionHandler);

  /**
   * Sets the connector's socket factory.
   * <p>
   * If no factory is configured that connector will use the platform's
   * default factory.
   *
   * @param socketFactory the factory to set
   */
  void setSocketFactory(SocketFactory socketFactory);

}
