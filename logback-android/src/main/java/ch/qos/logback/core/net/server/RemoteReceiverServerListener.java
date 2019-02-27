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

/**
 * A {@link ServerListener} that accepts connections from remote receiver
 * component clients.
 *
 * @author Carl Harris
 */
class RemoteReceiverServerListener
    extends ServerSocketListener<RemoteReceiverClient> {

  /**
   * Constructs a new listener.
   * @param serverSocket server socket from which new client connections
   *    will be accepted
   */
  public RemoteReceiverServerListener(ServerSocket serverSocket) {
    super(serverSocket);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected RemoteReceiverClient createClient(String id, Socket socket)
      throws IOException {
    return new RemoteReceiverStreamClient(id, socket);
  }

}
