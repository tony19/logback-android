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
import java.util.concurrent.Executor;

import javax.net.ServerSocketFactory;

import ch.qos.logback.classic.net.server.RemoteAppenderClient;
import ch.qos.logback.classic.net.server.RemoteAppenderServerListener;
import ch.qos.logback.classic.net.server.ServerSocketReceiver;
import ch.qos.logback.core.net.server.ServerListener;
import ch.qos.logback.core.net.server.ServerRunner;


/**
 * A {@link ServerSocketReceiver} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
public class InstrumentedServerSocketReceiver extends ServerSocketReceiver {

  private final ServerSocket serverSocket;
  private final ServerListener<RemoteAppenderClient> listener;
  private final ServerRunner<RemoteAppenderClient> runner;

  private ServerListener lastListener;

  public InstrumentedServerSocketReceiver(ServerSocket serverSocket) {
    this(serverSocket, new RemoteAppenderServerListener(serverSocket), null);
  }

  public InstrumentedServerSocketReceiver(ServerSocket serverSocket,
      ServerListener<RemoteAppenderClient> listener,
      ServerRunner<RemoteAppenderClient> runner) {
    this.serverSocket = serverSocket;
    this.listener = listener;
    this.runner = runner;
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
  protected ServerRunner createServerRunner(
      ServerListener<RemoteAppenderClient> listener,
      Executor executor) {
    lastListener = listener;
    return runner != null ? runner : super.createServerRunner(listener, executor);
  }

  @Override
  protected ServerListener<RemoteAppenderClient> createServerListener(
      ServerSocket socket) {
    return listener;
  }

  public ServerListener getLastListener() {
    return lastListener;
  }

}
