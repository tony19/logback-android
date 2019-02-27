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

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ExternalMockSocketServer {

  static final String LOGGINGEVENT = "LoggingEvent";
  static final String LOGGINGEVENT2 = "LoggingEvent2";
  static final String MINIMALEXT = "MinimalExt";
  static final String MINIMALSER = "MinimalSer";

  static final int PORT = 4560;

  //static int loopLen;
  static int clientNumber;

  static List<String> msgList = new ArrayList<String>();
  static boolean finished = false;

  String className = LOGGINGEVENT;

  public static void main(String[] args) {
    if (args.length == 1) {
      clientNumber = Integer.parseInt(args[0]);
      //loopLen = Integer.parseInt((args[1]));
      runServer();
    } else {
      usage("Wrong number of arguments.");
    }
  }

  static void usage(String msg) {
    System.err.println(msg);
    System.err
        .println("Usage: java " + ExternalMockSocketServer.class.getName()
            + " loopNumber");
    System.exit(1);
  }

  static void runServer() {

    try {
      System.out.println("Starting Server...");
      ServerSocket serverSocket = new ServerSocket(PORT);
      System.out.println("Listening on port " + PORT);
      for (int j = 0; j < clientNumber; j++) {
        Socket socket = serverSocket.accept();
        System.out.println("New client accepted.");
        System.out.println("Connected to client at " + socket.getInetAddress());

        InputStream is = socket.getInputStream();
        long sum = 0;
      
        while (true) {
          // this call is blocking
          int val = is.read();
          if(val == -1) {
            break;
          }
          // if a byte is available, we skip it.
          // this allows to pass all available bytes in a quick manner.
          int a = is.available();
          sum += a + 1;
          is.skip(a);
        }
        System.out.println(sum/1000 + " KB");
      }
      serverSocket.close();
    } catch (Exception se) {
      se.printStackTrace();
    }
    System.out.println("Server finished.");
    finished = true;
  }

}
