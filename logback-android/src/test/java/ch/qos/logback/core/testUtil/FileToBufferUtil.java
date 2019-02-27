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
package ch.qos.logback.core.testUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileToBufferUtil {

  static public void readIntoList(File file, List<String> stringList)
          throws IOException {

    if (file.getName().endsWith(".gz")) {
      gzFileReadIntoList(file, stringList);
    } else if (file.getName().endsWith(".zip")) {
      zipFileReadIntoList(file, stringList);
    } else {
      regularReadIntoList(file, stringList);
    }
  }

  private static void zipFileReadIntoList(File file, List<String> stringList) throws IOException {
  System.out.println("Reading zip file ["+file+"]");
    ZipFile zipFile = new ZipFile(file);
    Enumeration entries = zipFile.entries();
    ZipEntry entry = (ZipEntry) entries.nextElement();
    readInputStream(zipFile.getInputStream(entry), stringList);
  }

  static void readInputStream(InputStream is, List<String> stringList) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(is));
    String line;
    while ((line = in.readLine()) != null) {
      stringList.add(line);
    }
    in.close();
  }

  static public void regularReadIntoList(File file, List<String> stringList) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    BufferedReader in = new BufferedReader(new InputStreamReader(fis));
    String line;
    while ((line = in.readLine()) != null) {
      stringList.add(line);
    }
    in.close();
  }

  static public void gzFileReadIntoList(File file, List<String> stringList) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    GZIPInputStream gzis = new GZIPInputStream(fis);
    readInputStream(gzis, stringList);
  }

}
