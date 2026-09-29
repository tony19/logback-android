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

import static org.junit.Assert.assertEquals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SyslogOutputStream}.
 */
public class SyslogOutputStreamTest {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  /** Upper bound for receiving a datagram sent over the loopback interface. */
  private static final int RECEIVE_TIMEOUT = 10000;

  private DatagramSocket receiver;
  private SyslogOutputStream outputStream;

  @Before
  public void setUp() throws Exception {
    receiver = new DatagramSocket(0, InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
    receiver.setSoTimeout(RECEIVE_TIMEOUT);
    outputStream = new SyslogOutputStream("127.0.0.1", receiver.getLocalPort());
  }

  @After
  public void tearDown() throws Exception {
    outputStream.close();
    receiver.close();
  }

  @Test
  public void returnsTheConfiguredPort() {
    assertEquals(receiver.getLocalPort(), outputStream.getPort());
  }

  @Test
  public void sendsTheBytesWrittenSinceTheLastFlushAsOneDatagram() throws Exception {
    outputStream.write("hello".getBytes(UTF_8));
    outputStream.write('!');
    outputStream.flush();

    outputStream.write("again".getBytes(UTF_8));
    outputStream.flush();

    assertEquals("hello!", receive());
    assertEquals("again", receive());
  }

  @Test
  public void sendsLongMessagesWholeAndStartsAfreshAfterwards() throws Exception {
    byte[] longMessage = new byte[2000];
    Arrays.fill(longMessage, (byte) 'x');

    outputStream.write(longMessage);
    outputStream.flush();
    outputStream.write("short".getBytes(UTF_8));
    outputStream.flush();

    assertEquals(new String(longMessage, UTF_8), receive());
    assertEquals("short", receive());
  }

  @Test
  public void reportsTheSendBufferSizeOfItsDatagramSocket() throws Exception {
    DatagramSocket defaultSocket = new DatagramSocket();
    try {
      assertEquals(defaultSocket.getSendBufferSize(), outputStream.getSendBufferSize());
    } finally {
      defaultSocket.close();
    }
  }

  @Test
  public void sendsNoDatagramWhenNothingWasWritten() throws Exception {
    outputStream.flush();
    outputStream.write("message".getBytes(UTF_8));
    outputStream.flush();

    // the first datagram is the message, not an empty one
    assertEquals("message", receive());
  }

  @Test
  public void ignoresFlushAfterClose() throws Exception {
    outputStream.close();
    outputStream.write("dropped".getBytes(UTF_8));

    // does not fail although the datagram socket is gone
    outputStream.flush();
  }

  @Test
  public void canBeClosedTwice() {
    outputStream.close();

    // does not fail although the datagram socket is gone
    outputStream.close();
  }

  private String receive() throws Exception {
    byte[] buffer = new byte[4096];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    receiver.receive(packet);
    return new String(packet.getData(), packet.getOffset(), packet.getLength(), UTF_8);
  }
}
