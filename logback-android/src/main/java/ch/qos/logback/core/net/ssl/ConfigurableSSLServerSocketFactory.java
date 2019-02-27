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
package ch.qos.logback.core.net.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * An {@link SSLServerSocketFactory} that configures SSL parameters
 * (those specified in {@link SSLParametersConfiguration} on each newly
 * created socket.
 * <p>
 * When any of this factory's {@code createServerSocket} methods are invoked,
 * it calls on a delegate {@link SSLServerSocketFactory} to create the socket,
 * and then sets the SSL parameters of the socket (using the provided
 * configuration) before returning the socket to the caller.
 *
 * @author Carl Harris
 */
public class ConfigurableSSLServerSocketFactory extends ServerSocketFactory {

  private final SSLParametersConfiguration parameters;
  private final SSLServerSocketFactory delegate;

  /**
   * Creates a new factory.
   * @param parameters parameters that will be configured on each
   *    socket created by the factory
   * @param delegate socket factory that will be called upon to create
   *    server sockets before configuration
   */
  public ConfigurableSSLServerSocketFactory(
      SSLParametersConfiguration parameters, SSLServerSocketFactory delegate) {
    this.parameters = parameters;
    this.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
      throws IOException {
    SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(
        port, backlog, ifAddress);
    parameters.configure(new SSLConfigurableServerSocket(socket));
    return socket;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSocket createServerSocket(int port, int backlog)
      throws IOException {
    SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(
        port, backlog);
    parameters.configure(new SSLConfigurableServerSocket(socket));
    return socket;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSocket createServerSocket(int port) throws IOException {
    SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(
        port);
    parameters.configure(new SSLConfigurableServerSocket(socket));
    return socket;
  }

}
