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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import ch.qos.logback.core.util.CloseUtil;

/**
 * A {@link ServerListener} that accepts connections on a {@link ServerSocket}.
 *
 * @author Carl Harris
 */
public abstract class ServerSocketListener<T extends Client>
    implements ServerListener<T> {

  private final ServerSocket serverSocket;

  /**
   * Constructs a new listener.
   * @param serverSocket server socket delegate
   */
  public ServerSocketListener(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }

  /**
   * {@inheritDoc}
   */
  public T acceptClient() throws IOException {
    Socket socket = serverSocket.accept();
    return createClient(
        socketAddressToString(socket.getRemoteSocketAddress()), socket);
  }

  /**
   * Creates the client object for a new socket connection
   * @param id identifier string for the client
   * @param socket client's socket connection
   * @return client object
   * @throws IOException socket error
   */
  protected abstract T createClient(String id, Socket socket)
      throws IOException;

  /**
   * {@inheritDoc}
   */
  public void close() {
    CloseUtil.closeQuietly(serverSocket);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return socketAddressToString(serverSocket.getLocalSocketAddress());
  }

  /**
   * Converts a socket address to a reasonable display string.
   * @param address the subject socket address
   * @return display string
   */
  private String socketAddressToString(SocketAddress address) {
    String addr = address.toString();
    int i = addr.indexOf("/");
    if (i >= 0) {
      addr = addr.substring(i + 1);
    }
    return addr;
  }

}
