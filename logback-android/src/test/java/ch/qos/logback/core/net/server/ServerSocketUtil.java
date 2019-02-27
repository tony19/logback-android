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
import java.net.BindException;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

/**
 * Static utility methods for obtaining a {@link ServerSocket} bound to
 * a random unused port.
 *
 * @author Carl Harris
 */
public class ServerSocketUtil {

  /**
   * Creates a new {@link ServerSocket} bound to a random unused port.
   * <p>
   * This method is a convenience overload for
   * {@link #createServerSocket(ServerSocketFactory)} using the platform's
   * default {@link ServerSocketFactory}.
   * @return socket
   * @throws IOException
   */
  public static ServerSocket createServerSocket() throws IOException {
    return createServerSocket(ServerSocketFactory.getDefault());
  }

  /**
   * Creates a new {@link ServerSocket} bound to a random unused port.
   * @param socketFactory socket factory that will be used to create the
   *    socket
   * @return socket
   * @throws IOException
   */
  public static ServerSocket createServerSocket(
      ServerSocketFactory socketFactory) throws IOException {
    ServerSocket socket = null;
    int retries = 10;
    while (retries-- > 0 && socket == null) {
      int port = (int)((65536 - 1024) * Math.random()) + 1024;
      try {
        socket = socketFactory.createServerSocket(port);
      }
      catch (BindException ex) {
        // try again with different port
      }
    }
    if (socket == null) {
      throw new BindException("cannot find an unused port to bind");
    }
    return socket;
  }

}
