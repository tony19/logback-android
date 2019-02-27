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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.CloseUtil;

/**
 * A {@link RemoteReceiverClient} that writes serialized logging events to an
 * {@link OutputStream}.
 *
 * @author Carl Harris
 */
class RemoteReceiverStreamClient
    extends ContextAwareBase implements RemoteReceiverClient {

  private final String clientId;
  private final Socket socket;
  private final OutputStream outputStream;

  private BlockingQueue<Serializable> queue;

  /**
   * Constructs a new client.
   * @param id identifier string for the client
   * @param socket socket to which logging events will be written
   */
  public RemoteReceiverStreamClient(String id, Socket socket) {
    this.clientId = "client " + id + ": ";
    this.socket = socket;
    this.outputStream = null;
  }

  /**
   * Constructs a new client.
   * <p>
   * This constructor exists primarily to support unit tests where it
   * is inconvenient to have to create a socket for the test.
   *
   * @param id identifier string for the client
   * @param outputStream output stream to which logging Events will be written
   */
  RemoteReceiverStreamClient(String id, OutputStream outputStream) {
    this.clientId = "client " + id + ": ";
    this.socket = null;
    this.outputStream = outputStream;
  }

  /**
   * {@inheritDoc}
   */
  public void setQueue(BlockingQueue<Serializable> queue) {
    this.queue = queue;
  }

  /**
   * {@inheritDoc}
   */
  public boolean offer(Serializable event) {
    if (queue == null) {
      throw new IllegalStateException("client has no event queue");
    }
    return queue.offer(event);
  }

  /**
   * {@inheritDoc}
   */
  public void close() {
    if (socket == null) return;
    CloseUtil.closeQuietly(socket);
  }

  /**
   * {@inheritDoc}
   */
  public void run() {
    addInfo(clientId + "connected");

    ObjectOutputStream oos = null;
    try {
      int counter = 0;
      oos = createObjectOutputStream();
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Serializable event = queue.take();
          oos.writeObject(event);
          oos.flush();
          if (++counter >= CoreConstants.OOS_RESET_FREQUENCY) {
            // failing to reset the stream periodically will result in a
            // serious memory leak (as noted in AbstractSocketAppender)
            counter = 0;
            oos.reset();
          }
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
    catch (SocketException ex) {
      addInfo(clientId + ex);
    }
    catch (IOException ex) {
      addError(clientId + ex);
    }
    catch (RuntimeException ex) {
      addError(clientId + ex);
    }
    finally {
      if (oos != null) {
        CloseUtil.closeQuietly(oos);
      }
      close();
      addInfo(clientId + "connection closed");
    }
  }

  private ObjectOutputStream createObjectOutputStream() throws IOException {
    if (socket == null) {
      return new ObjectOutputStream(outputStream);
    }
    return new ObjectOutputStream(socket.getOutputStream());
  }
}
