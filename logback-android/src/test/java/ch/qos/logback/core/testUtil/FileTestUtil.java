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

import ch.qos.logback.core.util.CoreTestConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertTrue;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class FileTestUtil {

  private static final int BUFFER_SIZE = 1024;

  public static void makeTestOutputDir() {
    File target = new File(CoreTestConstants.TARGET_DIR);
    if(target.exists() && target.isDirectory()) {
      File testoutput = new File(CoreTestConstants.OUTPUT_DIR_PREFIX);
      if(!testoutput.exists())
        assertTrue(testoutput.mkdir());
    } else {
      throw new IllegalStateException(CoreTestConstants.TARGET_DIR + " does not exist");
    }
  }

  /**
   * Copies {@code src} over {@code dst}, closing both streams whether the copy
   * succeeds or fails. Test fixtures call this from setup methods, where a
   * failed copy that leaked its file handles would otherwise keep the source
   * file open (and, on Windows, locked) for the rest of the JVM's life.
   *
   * @param src file to read
   * @param dst file to overwrite
   * @throws IOException if either file cannot be opened, or the transfer fails
   */
  public static void copy(File src, File dst) throws IOException {
    copy(DEFAULT_STREAMS, src, dst);
  }

  /**
   * Opens the streams used by {@link #copy(File, File)}. Exists so that tests
   * can inject streams that fail on demand and record whether they were closed.
   */
  interface Streams {
    InputStream openInput(File file) throws IOException;
    OutputStream openOutput(File file) throws IOException;
  }

  static final Streams DEFAULT_STREAMS = new Streams() {
    public InputStream openInput(File file) throws IOException {
      return new FileInputStream(file);
    }

    public OutputStream openOutput(File file) throws IOException {
      return new FileOutputStream(file);
    }
  };

  static void copy(Streams streams, File src, File dst) throws IOException {
    // try-with-resources closes `in` even when openOutput() throws, and closes
    // `out` even when in.close() throws -- neither of which a plain
    // in.close(); out.close(); sequence at the end of the method would do.
    try (InputStream in = streams.openInput(src);
         OutputStream out = streams.openOutput(dst)) {
      byte[] buf = new byte[BUFFER_SIZE];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
    }
  }
}
