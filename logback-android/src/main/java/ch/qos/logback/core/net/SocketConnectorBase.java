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

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.SocketFactory;

/**
 * Base implementation of {@link SocketConnector}.
 *
 * @author Carl Harris
 */
public class SocketConnectorBase implements SocketConnector {

  /**
   * A strategy for choosing a delay after a failed connection attempt.
   */
  public interface DelayStrategy {
    int nextDelay();
  }

  private final Lock lock = new ReentrantLock();
  private final Condition connectCondition = lock.newCondition();

  private final InetAddress address;
  private final int port;

  private ExceptionHandler exceptionHandler;
  private SocketFactory socketFactory;
  private DelayStrategy delayStrategy;
  private Socket socket;

  /**
   * Constructs a new connector.
   * @param address address of remote listener
   * @param port port of remote listener
   * @param initialDelay delay before initial connection attempt
   * @param retryDelay delay after failed connection attempt
   */
  public SocketConnectorBase(InetAddress address, int port,
      int initialDelay, int retryDelay) {
    this(address, port, new FixedDelay(initialDelay, retryDelay));
  }

  /**
   * Constructs a new connector.
   * @param address address of remote listener
   * @param port port of remote listener
   * @param delayStrategy strategy for choosing the delay to impose before
   *    each connection attempt
   */
  public SocketConnectorBase(InetAddress address, int port,
      DelayStrategy delayStrategy) {
    this.address = address;
    this.port = port;
    this.delayStrategy = delayStrategy;
  }

  public void run() {
    if (socket != null) {
      throw new IllegalStateException("connector cannot be reused");
    }
    if (exceptionHandler == null) {
      exceptionHandler = new ConsoleExceptionHandler();
    }
    if (socketFactory == null) {
      socketFactory = SocketFactory.getDefault();
    }
    try {
      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(delayStrategy.nextDelay());
        try {
          socket = socketFactory.createSocket(address, port);
          signalConnected();
          break;
        }
        catch (Exception ex) {
          exceptionHandler.connectionFailed(this, ex);
        }
      }
    }
    catch (InterruptedException ex) {
      exceptionHandler.connectionFailed(this, ex);
    }
  }

  /**
   * Signals any threads waiting on {@code connectCondition} that the
   * connection has been established.
   */
  private void signalConnected() {
    lock.lock();
    try {
      connectCondition.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  public Socket awaitConnection() throws InterruptedException {
    return awaitConnection(Long.MAX_VALUE);
  }

  public Socket awaitConnection(long delay) throws InterruptedException {
    lock.lock();
    try {
      boolean timeout = false;
      while (socket == null && !timeout) {
        timeout = !connectCondition.await(delay, TimeUnit.MILLISECONDS);
      }
      return socket;
    }
    finally {
      lock.unlock();
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

  /**
   * A default {@link DelayStrategy} that implements a simple fixed delay.
   */
  private static class FixedDelay implements DelayStrategy {

    private final int retryDelay;
    private int nextDelay;

    public FixedDelay(int initialDelay, int retryDelay) {
      this.nextDelay = initialDelay;
      this.retryDelay = retryDelay;
    }

    public int nextDelay() {
      int delay = nextDelay;
      nextDelay = retryDelay;
      return delay;
    }

  }

  public Socket call() throws InterruptedException {
    // TODO Auto-generated method stub
    return null;
  }

}
