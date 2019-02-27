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
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * A {@link AbstractServerSocketAppender} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
public class InstrumentedServerSocketAppenderBase
    extends AbstractServerSocketAppender<Serializable> {

  private final ServerSocket serverSocket;
  private final ServerListener<RemoteReceiverClient> listener;
  private final ServerRunner<RemoteReceiverClient> runner;

  private ServerListener lastListener;

  public InstrumentedServerSocketAppenderBase(ServerSocket serverSocket) {
    this(serverSocket, new RemoteReceiverServerListener(serverSocket), null);
  }

  public InstrumentedServerSocketAppenderBase(ServerSocket serverSocket,
      ServerListener<RemoteReceiverClient> listener,
      ServerRunner<RemoteReceiverClient> runner) {
    this.serverSocket = serverSocket;
    this.listener = listener;
    this.runner = runner;
  }

  @Override
  protected void postProcessEvent(Serializable event) {
  }

  @Override
  protected PreSerializationTransformer<Serializable> getPST() {
    return new PreSerializationTransformer<Serializable>() {
      public Serializable transform(Serializable event) {
        return event;
      }
    };
  }

  @Override
  protected ServerSocketFactory getServerSocketFactory() throws Exception {
    return new ServerSocketFactory() {

      @Override
      public ServerSocket createServerSocket(int port) throws IOException {
        return serverSocket;
      }

      @Override
      public ServerSocket createServerSocket(int port, int backlog)
          throws IOException {
        return serverSocket;
      }

      @Override
      public ServerSocket createServerSocket(int port, int backlog,
          InetAddress ifAddress) throws IOException {
        return serverSocket;
      }
    };
  }

  @Override
  protected ServerRunner<RemoteReceiverClient> createServerRunner(
      ServerListener<RemoteReceiverClient> listener, Executor executor) {
    lastListener = listener;
    return runner != null ? runner : super.createServerRunner(listener, executor);
  }

  @Override
  protected ServerListener<RemoteReceiverClient> createServerListener(
      ServerSocket socket) {
    return listener;
  }

  public ServerListener getLastListener() {
    return lastListener;
  }

}
