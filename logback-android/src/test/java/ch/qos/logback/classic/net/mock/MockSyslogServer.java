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
package ch.qos.logback.classic.net.mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class MockSyslogServer extends Thread {

  final int loopLen;
  final int port;

  List<byte[]> msgList = new ArrayList<byte[]>();
  boolean finished = false;
  
  public MockSyslogServer(int loopLen, int port) {
    super();
    this.loopLen = loopLen;
    this.port = port;
  }

  @Override
  public void run() {
    //System.out.println("MockSyslogServer listening on port "+port);
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket(port);

      for (int i = 0; i < loopLen; i++) {
        byte[] buf = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        //System.out.println("Waiting for message");
        socket.receive(packet);
        byte[] out = new byte[packet.getLength()];
        System.arraycopy(buf, 0, out, 0, out.length);
        msgList.add(out);
      }
    } catch (Exception se) {
      se.printStackTrace();
    } finally {
      if(socket != null) {
    try {socket.close();} catch(Exception e) {}
      }
    }
    finished = true;
  }
  
  public boolean isFinished() {
    return finished;
  }

  public List<byte[]> getMessageList() {
    return msgList;
  }
}
