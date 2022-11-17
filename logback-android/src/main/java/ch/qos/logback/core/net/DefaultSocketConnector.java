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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import ch.qos.logback.core.util.DelayStrategy;
import ch.qos.logback.core.util.FixedDelay;

/**
 * Default implementation of {@link SocketConnector}.
 *
 * @author Carl Harris
 * @since 1.0.12
 */
public class DefaultSocketConnector implements SocketConnector {

  private final InetAddress address;
  private final int port;
  private final DelayStrategy delayStrategy;

  private ExceptionHandler exceptionHandler;
  private SocketFactory socketFactory;

  /**
   * Constructs a new connector.
   *
   * @param address      address of remote listener
   * @param port         port of remote listener
   * @param initialDelay delay before initial connection attempt
   * @param retryDelay   delay after failed connection attempt
   */
  public DefaultSocketConnector(InetAddress address, int port,
                                long initialDelay, long retryDelay) {
    this(address, port, new FixedDelay(initialDelay, retryDelay));
  }

  /**
   * Constructs a new connector.
   *
   * @param address       address of remote listener
   * @param port          port of remote listener
   * @param delayStrategy strategy for choosing the delay to impose before
   *                      each connection attempt
   */
  public DefaultSocketConnector(InetAddress address, int port,
                                DelayStrategy delayStrategy) {
    this.address = address;
    this.port = port;
    this.delayStrategy = delayStrategy;
  }

  /**
   * Loops until the desired connection is established and returns the resulting connector.
   */
  public Socket call() throws InterruptedException {
    useDefaultsForMissingFields();
    Socket socket = createSocket();
    while (socket == null && !Thread.currentThread().isInterrupted()) {
      Thread.sleep(delayStrategy.nextDelay());
      socket = createSocket();
    }
    return socket;
  }

  private Socket createSocket() {
    Socket newSocket = null;
    try {
      newSocket = socketFactory.createSocket(address, port);
    } catch (IOException ioex) {
      exceptionHandler.connectionFailed(this, ioex);
    }
    return newSocket;
  }

  private void useDefaultsForMissingFields() {
    if (exceptionHandler == null) {
      exceptionHandler = new ConsoleExceptionHandler();
    }
    if (socketFactory == null) {
      socketFactory = SocketFactory.getDefault();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setExceptionHandler(ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * {@inheritDoc}
   */
  public void setSocketFactory(SocketFactory socketFactory) {
    this.socketFactory = socketFactory;
  }

  /**
   * A default {@link ExceptionHandler} that writes to {@code System.out}
   */
  private static class ConsoleExceptionHandler implements ExceptionHandler {

    public void connectionFailed(SocketConnector connector, Exception ex) {
      System.out.println(ex);
    }

  }

}
