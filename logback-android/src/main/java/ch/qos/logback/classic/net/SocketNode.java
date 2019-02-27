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
package ch.qos.logback.classic.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import ch.qos.logback.classic.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.server.HardenedLoggingEventInputStream;
import ch.qos.logback.classic.spi.ILoggingEvent;

// Contributors: Moses Hohman <mmhohman@rainbow.uchicago.edu>

/**
 * Read {@link ILoggingEvent} objects sent from a remote client using Sockets
 * (TCP). These logging events are logged according to local policy, as if they
 * were generated locally.
 *
 * <p>
 * For example, the socket node might decide to log events to a local file and
 * also resent them to a second socket node.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 *
 * @since 0.8.4
 */
public class SocketNode implements Runnable {

  Socket socket;
  LoggerContext context;
  HardenedLoggingEventInputStream hardenedLoggingEventInputStream;
  SocketAddress remoteSocketAddress;

  Logger logger;
  boolean closed = false;
  SimpleSocketServer socketServer;

  public SocketNode(SimpleSocketServer socketServer, Socket socket, LoggerContext context) {
    this.socketServer = socketServer;
    this.socket = socket;
    remoteSocketAddress = socket.getRemoteSocketAddress();
    this.context = context;
    logger = context.getLogger(SocketNode.class);
  }

  // public
  // void finalize() {
  // System.err.println("-------------------------Finalize called");
  // System.err.flush();
  // }

  public void run() {

    try {
      hardenedLoggingEventInputStream = new HardenedLoggingEventInputStream(new BufferedInputStream(socket.getInputStream()));
    } catch (Exception e) {
      logger.error("Could not open ObjectInputStream to " + socket, e);
      closed = true;
    }

    ILoggingEvent event;
    Logger remoteLogger;

    try {
      while (!closed) {
        // read an event from the wire
        event = (ILoggingEvent) hardenedLoggingEventInputStream.readObject();
        // get a logger from the hierarchy. The name of the logger is taken to
        // be the name contained in the event.
        remoteLogger = context.getLogger(event.getLoggerName());
        // apply the logger-level filter
        if (remoteLogger.isEnabledFor(event.getLevel())) {
          // finally log the event as if was generated locally
          remoteLogger.callAppenders(event);
        }
      }
    } catch (java.io.EOFException e) {
      logger.info("Caught java.io.EOFException closing connection.");
    } catch (java.net.SocketException e) {
      logger.info("Caught java.net.SocketException closing connection.");
    } catch (IOException e) {
      logger.info("Caught java.io.IOException: " + e);
      logger.info("Closing connection.");
    } catch (Exception e) {
      logger.error("Unexpected exception. Closing connection.", e);
    }

    socketServer.socketNodeClosing(this);
    close();
  }

  void close() {
    if(closed) {
      return;
    }
    closed = true;
    if (hardenedLoggingEventInputStream != null) {
      try {
        hardenedLoggingEventInputStream.close();
      } catch (IOException e) {
        logger.warn("Could not close connection.", e);
      } finally {
        hardenedLoggingEventInputStream = null;
      }
    }
  }

  @Override
  public String toString() {
    return this.getClass().getName()+remoteSocketAddress.toString();
  }
}
