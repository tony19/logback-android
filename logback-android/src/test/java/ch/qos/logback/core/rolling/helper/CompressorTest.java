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
package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

public class CompressorTest {

  private static final String CONTENT = "some log data\n";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private final Context context = new ContextBase();

  private File source;

  @Before
  public void setUp() throws IOException {
    source = tmpDir.newFile("app.log");
    write(source, CONTENT);
  }

  @Test
  public void compressingInNoneModeIsUnsupported() {
    Compressor compressor = compressor(CompressionMode.NONE);

    UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
        () -> compressor.compress(source.getPath(), source.getPath() + ".gz", "app.log"));

    assertEquals("compress method called in NONE compression mode", e.getMessage());
    assertTrue(source.exists());
  }

  @Test
  public void fileNameWithoutCompressionSuffixIsReturnedUnchanged() {
    assertEquals("app.log", Compressor.computeFileNameStrWithoutCompSuffix("app.log.gz", CompressionMode.GZ));
    assertEquals("app.log", Compressor.computeFileNameStrWithoutCompSuffix("app.log", CompressionMode.GZ));
    assertEquals("app.log.zip", Compressor.computeFileNameStrWithoutCompSuffix("app.log.zip", CompressionMode.GZ));
    assertEquals("app.log", Compressor.computeFileNameStrWithoutCompSuffix("app.log.zip", CompressionMode.ZIP));
    assertEquals("app.log.gz", Compressor.computeFileNameStrWithoutCompSuffix("app.log.gz", CompressionMode.ZIP));
    assertEquals("app.log.gz", Compressor.computeFileNameStrWithoutCompSuffix("app.log.gz", CompressionMode.NONE));
  }

  private Compressor compressor(CompressionMode mode) {
    Compressor compressor = new Compressor(mode);
    compressor.setContext(context);
    return compressor;
  }

  private static void write(File file, String content) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(content.getBytes("UTF-8"));
    } finally {
      fos.close();
    }
  }
}
