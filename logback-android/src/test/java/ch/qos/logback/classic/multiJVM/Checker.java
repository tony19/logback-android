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
package ch.qos.logback.classic.multiJVM;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Checker {

  static long LEN;
  static String FILENAME;

  static void usage(String msg) {
    System.err.println(msg);
    System.err
        .println("Usage: java "
            + Checker.class.getName()
            + " runLength filename stamp0 stamp1 ..stampN\n"
            + "   runLength (integer) the number of logs to generate perthread\n"
            + "    filename (string) the filename where to write\n"
            + "   stamp0 JVM instance stamp0\n"
            + "   stamp1 JVM instance stamp1\n");
    System.exit(1);
  }

  public static void main(String[] argv) throws Exception {
    if (argv.length < 3) {
      usage("Wrong number of arguments.");
    }

    LEN = Integer.parseInt(argv[0]);
    FILENAME = argv[1];

    for (int i = 2; i < argv.length; i++) {
      check(argv[i], FILENAME, true);
    }
  }

  static void check(String stamp, String filename, boolean safetyMode)
      throws Exception {

    FileReader fr = new FileReader(FILENAME);
    BufferedReader br = new BufferedReader(fr);

    try {
      String regExp = "^" + stamp + " DEBUG - " + LoggingThread.msgLong
              + " (\\d+)$";
      Pattern p = Pattern.compile(regExp);

      String line;
      int expected = 0;
      while ((line = br.readLine()) != null) {
        Matcher m = p.matcher(line);
        if (m.matches()) {
          String g = m.group(1);
          int num = Integer.parseInt(g);
          if (num != expected) {
            System.err.println("ERROR: out of sequence line: ");
            System.err.println(line);
            return;
          }
          expected++;
        }
      }

      if (expected != LEN) {
        System.err.println("ERROR: For JVM stamp " + stamp + " found " + expected
                + " was expecting " + LEN);
      } else {
        System.out.println("For JVM stamp " + stamp + " found " + LEN
                + " lines in correct sequence");
      }
    } finally {
      fr.close();
      br.close();
    }
  }
}