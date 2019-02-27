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
package ch.qos.logback.core.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class Compare {

  public static boolean compare(String file1, String file2) throws IOException {
    if (file1.endsWith(".gz")) {
      return gzFileCompare(file1, file2);
    } else if(file1.endsWith(".zip")) {
      return zipFileCompare(file1, file2);
    } else {
      return regularFileCompare(file1, file2);
    }
  }

  static BufferedReader gzFileToBufferedReader(String file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    GZIPInputStream gzis = new GZIPInputStream(fis);
    return new BufferedReader(new InputStreamReader(gzis));
  }

  static BufferedReader zipFileToBufferedReader(String file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    ZipInputStream zis = new  ZipInputStream(fis);
    zis.getNextEntry();
    return new BufferedReader(new InputStreamReader(zis));
  }
  public static boolean gzFileCompare(String file1, String file2) throws IOException {
    BufferedReader in1 = gzFileToBufferedReader(file1);
    BufferedReader in2 = gzFileToBufferedReader(file2);
    return bufferCompare(in1, in2, file1, file2);
  }

  public static boolean zipFileCompare(String file1, String file2) throws IOException {
    BufferedReader in1 = zipFileToBufferedReader(file1);
    BufferedReader in2 = zipFileToBufferedReader(file2);
    return bufferCompare(in1, in2, file1, file2);
  }
  public static boolean regularFileCompare(String file1, String file2)
      throws IOException {
    BufferedReader in1 = new BufferedReader(new FileReader(file1));
    BufferedReader in2 = new BufferedReader(new FileReader(file2));
    return bufferCompare(in1, in2, file1, file2);
  }

  public static boolean bufferCompare(BufferedReader in1, BufferedReader in2,
      String file1, String file2) throws IOException {

    String s1;
    int lineCounter = 0;

    while ((s1 = in1.readLine()) != null) {
      lineCounter++;

      String s2 = in2.readLine();

      if (!s1.equals(s2)) {
        System.out.println("Files [" + file1 + "] and [" + file2
            + "] differ on line " + lineCounter);
        System.out.println("One reads:  [" + s1 + "].");
        System.out.println("Other reads:[" + s2 + "].");
        return false;
      }
    }

    // the second file is longer
    if (in2.read() != -1) {
      System.out.println("File [" + file2 + "] longer than file [" + file1
          + "].");
      return false;
    }

    return true;
  }

  public static boolean gzCompare(String file1, String file2) throws IOException {
    BufferedReader in1 = null;
    BufferedReader in2 = null;

    try {
      in1 = new BufferedReader(new InputStreamReader(
              new GZIPInputStream(new FileInputStream(file1))));
      in2 = new BufferedReader(new InputStreamReader(
              new GZIPInputStream(new FileInputStream(file2))));

      String s1;
      int lineCounter = 0;

      while ((s1 = in1.readLine()) != null) {
        lineCounter++;

        String s2 = in2.readLine();

        if (!s1.equals(s2)) {
          System.out.println("Files [" + file1 + "] and [" + file2
                  + "] differ on line " + lineCounter);
          System.out.println("One reads:  [" + s1 + "].");
          System.out.println("Other reads:[" + s2 + "].");
          return false;
        }
      }

      // the second file is longer
      if (in2.read() != -1) {
        System.out.println("File [" + file2 + "] longer than file [" + file1
                + "].");
        return false;
      }

      return true;
    } finally {
      close(in1);
      close(in2);
    }
  }

  static void close(Reader r) {
    if (r != null)
      try {
        r.close();
      } catch (IOException e) {
      }
  }

}
