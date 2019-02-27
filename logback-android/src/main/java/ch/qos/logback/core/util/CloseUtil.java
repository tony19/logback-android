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
package ch.qos.logback.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Static utility method for {@link Closeable} objects.
 *
 * @author Carl Harris
 */
public class CloseUtil {

  /**
   * Closes a closeable while suppressing any {@code IOException} that occurs.
   * @param closeable the socket to close
   */
  public static void closeQuietly(Closeable closeable) {
    if (closeable == null) return;
    try {
      closeable.close();
    }
    catch (IOException ex) {
      assert true;  // avoid an empty catch
    }
  }

  /**
   * Closes a socket while suppressing any {@code IOException} that occurs.
   * @param socket the socket to close
   */
  public static void closeQuietly(Socket socket) {
    if (socket == null) return;
    try {
      socket.close();
    }
    catch (IOException ex) {
      assert true;  // avoid an empty catch
    }
  }

  /**
   * Closes a server socket while suppressing any {@code IOException} that
   * occurs.
   * @param serverSocket the socket to close
   */
  public static void closeQuietly(ServerSocket serverSocket) {
    if (serverSocket == null) return;
    try {
      serverSocket.close();
    }
    catch (IOException ex) {
      assert true;  // avoid an empty catch
    }
  }

}
