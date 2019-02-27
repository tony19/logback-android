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

import java.io.*;
import java.nio.channels.FileChannel;

public class ResilientFileOutputStream extends ResilientOutputStreamBase {

  private File file;
  private FileOutputStream fos;


  public ResilientFileOutputStream(File file, boolean append, long bufferSize) throws FileNotFoundException {
    this.file = file;
    fos = new FileOutputStream(file, append);
    this.os = new BufferedOutputStream(fos, (int) bufferSize);
    this.presumedClean = true;
  }

  public FileChannel getChannel() {
    if (os == null) {
      return null;
    }
    return fos.getChannel();
  }

  public File getFile() {
    return file;
  }

  @Override
  String getDescription() {
    return "file ["+file+"]";
  }

  @Override
  OutputStream openNewOutputStream() throws IOException {
    // see LOGBACK-765
    fos = new FileOutputStream(file, true);
    return new BufferedOutputStream(fos);
  }
  
  @Override
  public String toString() {
    return "c.q.l.c.recovery.ResilientFileOutputStream@"
        + System.identityHashCode(this);
  }

}
