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
package ch.qos.logback.classic.net.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import ch.qos.logback.classic.net.ReceiverBase;
import ch.qos.logback.core.net.AbstractSocketAppender;
import ch.qos.logback.core.net.server.ServerListener;
import ch.qos.logback.core.net.server.ServerRunner;
import ch.qos.logback.core.util.CloseUtil;

/**
 * A logging socket server that is configurable using Joran.
 *
 * @author Carl Harris
 */
public class ServerSocketReceiver extends ReceiverBase {

  /**
   * Default {@link ServerSocket} backlog
   */
  public static final int DEFAULT_BACKLOG = 50;

  private int port = AbstractSocketAppender.DEFAULT_PORT;
  private int backlog = DEFAULT_BACKLOG;
  private String address;

  private ServerRunner runner;

  /**
   * Starts the server.
   */
  protected boolean shouldStart() {
    ServerSocket serverSocket = null;
    try {
      serverSocket = getServerSocketFactory().createServerSocket(
          getPort(), getBacklog(), getInetAddress());

      ServerListener<RemoteAppenderClient> listener =
          createServerListener(serverSocket);

      runner = createServerRunner(listener, getContext().getScheduledExecutorService());
      runner.setContext(getContext());
      return true;
    }
    catch (Exception ex) {
      addError("server startup error: " + ex, ex);
      CloseUtil.closeQuietly(serverSocket);
      return false;
    }
  }

  protected ServerListener<RemoteAppenderClient> createServerListener(
      ServerSocket socket) {
    return new RemoteAppenderServerListener(socket);
  }

  protected ServerRunner createServerRunner(
      ServerListener<RemoteAppenderClient> listener,
      Executor executor) {
    return new RemoteAppenderServerRunner(listener, executor);
  }

  @Override
  protected Runnable getRunnableTask() {
    return runner;
  }

  /**
   * {@inheritDoc}
   */
  protected void onStop() {
    try {
      if (runner == null) return;
      runner.stop();
    }
    catch (IOException ex) {
      addError("server shutdown error: " + ex, ex);
    }
  }

  /**
   * Gets the server socket factory.
   * <p>
   * Subclasses may override to provide a custom factory.
   * @return server socket factory
   * @throws Exception
   */
  protected ServerSocketFactory getServerSocketFactory() throws Exception {
    return ServerSocketFactory.getDefault();
  }

  /**
   * Gets the local address for the listener.
   * @return an {@link InetAddress} representation of the local address.
   * @throws UnknownHostException
   */
  protected InetAddress getInetAddress() throws UnknownHostException {
    if (getAddress() == null) return null;
    return InetAddress.getByName(getAddress());
  }

  /**
   * Gets the local port for the listener.
   * @return local port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the local port for the listener.
   * @param port the local port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets the listener queue depth.
   * <p>
   * This represents the number of connected clients whose connections
   * have not yet been accepted.
   * @return queue depth
   * @see java.net.ServerSocket
   */
  public int getBacklog() {
    return backlog;
  }

  /**
   * Sets the listener queue depth.
   * <p>
   * This represents the number of connected clients whose connections
   * have not yet been accepted.
   * @param backlog the queue depth to set
   * @see java.net.ServerSocket
   */
  public void setBacklog(int backlog) {
    this.backlog = backlog;
  }

  /**
   * Gets the local address for the listener.
   * @return a string representation of the local address
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the local address for the listener.
   * @param address a host name or a string representation of an IP address
   */
  public void setAddress(String address) {
    this.address = address;
  }

}
