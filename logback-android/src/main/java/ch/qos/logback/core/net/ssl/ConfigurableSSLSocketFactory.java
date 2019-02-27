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
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link SSLSocketFactory} that configures SSL parameters
 * (those covered by {@link SSLParameters}) on each newly created socket.
 * <p>
 * When any of this factory's {@code createSocket} methods are invoked, it
 * calls on a {@link SSLSocketFactory} delegate to create the socket, and
 * then sets the SSL parameters of the socket (using the provided
 * configuration) before returning the socket to the caller.
 *
 * @author Carl Harris
 */
public class ConfigurableSSLSocketFactory extends SocketFactory {

  private final SSLParametersConfiguration parameters;
  private final SSLSocketFactory delegate;

  /**
   * Creates a new factory.
   * @param parameters parameters that will be configured on each
   *    socket created by the factory
   * @param delegate socket factory that will be called upon to create
   *    sockets before configuration
   */
  public ConfigurableSSLSocketFactory(SSLParametersConfiguration parameters,
      SSLSocketFactory delegate) {
    this.parameters = parameters;
    this.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket(InetAddress address, int port,
      InetAddress localAddress, int localPort) throws IOException {
    SSLSocket socket = (SSLSocket) delegate.createSocket(address, port,
        localAddress, localPort);
    parameters.configure(new SSLConfigurableSocket(socket));
    return socket;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
    parameters.configure(new SSLConfigurableSocket(socket));
    return socket;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket(String host, int port, InetAddress localHost,
      int localPort) throws IOException, UnknownHostException {
    SSLSocket socket = (SSLSocket) delegate.createSocket(host, port,
        localHost, localPort);
    parameters.configure(new SSLConfigurableSocket(socket));
    return socket;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket(String host, int port) throws IOException,
      UnknownHostException {
    SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
    parameters.configure(new SSLConfigurableSocket(socket));
    return socket;
  }

}
