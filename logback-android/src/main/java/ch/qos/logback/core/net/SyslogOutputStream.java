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
package ch.qos.logback.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * SyslogOutputStream is a wrapper around the {@link DatagramSocket} class so that it
 * behaves like an {@link OutputStream}.
 */
public class SyslogOutputStream extends OutputStream {

  /**
   * The maximum length after which we discard the existing string buffer and
   * start anew.
   */
  private static final int MAX_LEN = 1024;

  private InetAddress address;
  private DatagramSocket ds;
  private ByteArrayOutputStream baos = new ByteArrayOutputStream();
  final private int port;

  public SyslogOutputStream(String syslogHost, int port) throws UnknownHostException,
      SocketException {
    this.address = InetAddress.getByName(syslogHost);
    this.port = port;
    this.ds = new DatagramSocket();
  }

  public void write(byte[] byteArray, int offset, int len) throws IOException {
    baos.write(byteArray, offset, len);
  }

  public void flush() throws IOException {
    byte[] bytes = baos.toByteArray();
    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address,
        port);

    // clean up for next round
    if (baos.size() > MAX_LEN) {
      baos = new ByteArrayOutputStream();
    } else {
      baos.reset();
    }
    
    // after a failure, it can happen that bytes.length is zero
    // in that case, there is no point in sending out an empty message/
    if(bytes.length == 0) {
      return;
    }
    if (this.ds != null) {
      ds.send(packet);
    }
  
  }

  public void close() {
    if (ds != null) {
      ds.close();
    }
    address = null;
    ds = null;
  }

  public int getPort() {
    return port;
  }

  @Override
  public void write(int b) throws IOException {
    baos.write(b);
  }

  int getSendBufferSize() throws SocketException {
    return ds.getSendBufferSize();
  }
}
