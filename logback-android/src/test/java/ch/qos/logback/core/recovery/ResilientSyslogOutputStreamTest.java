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
package ch.qos.logback.core.recovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.SyslogOutputStream;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class ResilientSyslogOutputStreamTest {

  static final String LOOPBACK = "127.0.0.1";
  static final Charset UTF_8 = Charset.forName("UTF-8");

  Context context = new ContextBase();
  DatagramSocket receiver;
  int port;
  ResilientSyslogOutputStream stream;

  @Before
  public void setUp() throws IOException {
    receiver = new DatagramSocket(0, InetAddress.getByName(LOOPBACK));
    receiver.setSoTimeout(10000);
    port = receiver.getLocalPort();
    stream = new ResilientSyslogOutputStream(LOOPBACK, port);
    stream.setContext(context);
  }

  @After
  public void tearDown() throws IOException {
    stream.close();
    receiver.close();
  }

  private String receive() throws IOException {
    byte[] buffer = new byte[1024];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    receiver.receive(packet);
    return new String(packet.getData(), packet.getOffset(), packet.getLength(), UTF_8);
  }

  private void send(String message) {
    byte[] bytes = message.getBytes(UTF_8);
    stream.write(bytes, 0, bytes.length);
    stream.flush();
  }

  @Test
  public void flushedBytesAreSentToSyslogHost() throws IOException {
    send("hello");

    assertEquals("hello", receive());
  }

  @Test
  public void descriptionNamesHostAndPort() {
    assertEquals("syslog [" + LOOPBACK + ":" + port + "]", stream.getDescription());
  }

  @Test
  public void recoveryOpensNewStreamToSameHostAndPort() throws IOException {
    OutputStream original = stream.os;

    stream.attemptRecovery();

    assertNotSame(original, stream.os);
    assertTrue(stream.os instanceof SyslogOutputStream);
    assertEquals(port, ((SyslogOutputStream) stream.os).getPort());
    new StatusChecker(context).assertContainsMatch(Status.INFO,
        "Attempting to recover from IO failure on syslog \\[" + LOOPBACK + ":" + port + "\\]");
    send("again");
    assertEquals("again", receive());
  }

  @Test
  public void toStringIdentifiesTheInstance() {
    assertEquals("c.q.l.c.recovery.ResilientSyslogOutputStream@" + System.identityHashCode(stream),
        stream.toString());
  }
}
