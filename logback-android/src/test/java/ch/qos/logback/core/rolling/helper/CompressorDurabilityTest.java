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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Tests that an interrupted or failed compression can never lose log data or
 * leave a malformed archive behind (issue #369).
 */
public class CompressorDurabilityTest {

  private static final String CONTENT = "hello rollover durability\n";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private final Context context = new ContextBase();
  private StatusChecker checker;

  @Before
  public void setup() {
    checker = new StatusChecker(context);
  }

  private Compressor gzCompressor() {
    Compressor compressor = new Compressor(CompressionMode.GZ);
    compressor.setContext(context);
    return compressor;
  }

  private File createSourceFile(String name) throws IOException {
    File source = tmpDir.newFile(name);
    FileOutputStream fos = new FileOutputStream(source);
    fos.write(CONTENT.getBytes("UTF-8"));
    fos.close();
    return source;
  }

  private String gunzip(File gzFile) throws IOException {
    GZIPInputStream in = new GZIPInputStream(new FileInputStream(gzFile));
    try {
      return readAll(in);
    } finally {
      in.close();
    }
  }

  private String readAll(java.io.InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int n;
    while ((n = in.read(buf)) != -1) {
      out.write(buf, 0, n);
    }
    return out.toString("UTF-8");
  }

  @Test
  public void successfulGzCompressionLeavesNoTempFile() throws IOException {
    File source = createSourceFile("app.log");
    File target = new File(tmpDir.getRoot(), "app.log.gz");

    gzCompressor().compress(source.getPath(), target.getPath(), null);

    assertTrue(checker.isErrorFree(0));
    assertTrue("archive missing", target.exists());
    assertFalse("temp file left behind",
        new File(target.getPath() + Compressor.TMP_SUFFIX).exists());
    assertFalse("source not deleted", source.exists());
    assertEquals(CONTENT, gunzip(target));
  }

  @Test
  public void failedGzCompressionPreservesSourceFile() throws IOException {
    File source = createSourceFile("app.log");
    // parent "directory" of the target is a regular file, so opening the
    // temp file for the archive must fail
    File blocker = tmpDir.newFile("blocked");
    File target = new File(blocker, "app.log.gz");

    gzCompressor().compress(source.getPath(), target.getPath(), null);

    assertTrue("compression failure must be reported",
        checker.containsMatch("Error occurred while compressing"));
    assertTrue("source file must survive a failed compression", source.exists());
    assertEquals(CONTENT, readAll(new FileInputStream(source)));
    assertFalse("no archive may exist after a failure", target.exists());
    assertFalse("no temp file may remain after a failure",
        new File(target.getPath() + Compressor.TMP_SUFFIX).exists());
  }

  @Test
  public void interruptedCompressionCanBeRetried() throws IOException {
    File source = createSourceFile("app.log");
    File target = new File(tmpDir.getRoot(), "app.log.gz");

    // a partial temp file left behind by a previous, abruptly killed
    // compression of the same target
    File staleTmp = new File(target.getPath() + Compressor.TMP_SUFFIX);
    FileOutputStream fos = new FileOutputStream(staleTmp);
    fos.write("garbage from a killed process".getBytes("UTF-8"));
    fos.close();

    gzCompressor().compress(source.getPath(), target.getPath(), null);

    assertTrue(checker.isErrorFree(0));
    assertTrue(target.exists());
    assertFalse(staleTmp.exists());
    assertEquals(CONTENT, gunzip(target));
  }

  @Test
  public void staleTempFilesAreSwept() throws IOException {
    // orphaned partial archives from compressions of other targets, killed
    // long ago (mtime beyond the stale threshold)
    File staleGz = tmpDir.newFile("old-period.log.gz" + Compressor.TMP_SUFFIX);
    File staleZip = tmpDir.newFile("old-period.log.zip" + Compressor.TMP_SUFFIX);
    long oldTime = System.currentTimeMillis() - Compressor.STALE_TMP_AGE_MS - 60_000;
    assertTrue(staleGz.setLastModified(oldTime));
    assertTrue(staleZip.setLastModified(oldTime));

    // a fresh temp file (e.g. a concurrent in-flight compression) that must
    // NOT be swept
    File freshTmp = tmpDir.newFile("in-flight.log.gz" + Compressor.TMP_SUFFIX);

    // a source-side temp file created by TimeBasedRollingPolicy's
    // renameRawAndAsyncCompress: contains original log data (name has
    // digits between the compression suffix and .tmp), must NOT be swept
    File rawTmp = tmpDir.newFile("app.log.gz1234567890" + Compressor.TMP_SUFFIX);
    assertTrue(rawTmp.setLastModified(oldTime));

    File source = createSourceFile("app.log");
    File target = new File(tmpDir.getRoot(), "app.log.gz");
    gzCompressor().compress(source.getPath(), target.getPath(), null);

    assertTrue(checker.isErrorFree(0));
    assertFalse("stale gz temp file not swept", staleGz.exists());
    assertFalse("stale zip temp file not swept", staleZip.exists());
    assertTrue("fresh temp file must not be swept", freshTmp.exists());
    assertTrue("uncompressed raw temp file (log data!) must not be swept", rawTmp.exists());
  }

  @Test
  public void successfulZipCompressionLeavesNoTempFile() throws IOException {
    File source = createSourceFile("app.log");
    File target = new File(tmpDir.getRoot(), "app.log.zip");

    Compressor compressor = new Compressor(CompressionMode.ZIP);
    compressor.setContext(context);
    compressor.compress(source.getPath(), target.getPath(), "app.log");

    assertTrue(checker.isErrorFree(0));
    assertTrue(target.exists());
    assertFalse(new File(target.getPath() + Compressor.TMP_SUFFIX).exists());
    assertFalse(source.exists());

    ZipInputStream zin = new ZipInputStream(new FileInputStream(target));
    try {
      ZipEntry entry = zin.getNextEntry();
      assertEquals("app.log", entry.getName());
      assertEquals(CONTENT, readAll(zin));
    } finally {
      zin.close();
    }
  }

  @Test
  public void failedZipCompressionPreservesSourceFile() throws IOException {
    File source = createSourceFile("app.log");
    File blocker = tmpDir.newFile("blocked");
    File target = new File(blocker, "app.log.zip");

    Compressor compressor = new Compressor(CompressionMode.ZIP);
    compressor.setContext(context);
    compressor.compress(source.getPath(), target.getPath(), "app.log");

    assertTrue(checker.containsMatch("Error occurred while compressing"));
    assertTrue("source file must survive a failed compression", source.exists());
    assertFalse(target.exists());
    assertFalse(new File(target.getPath() + Compressor.TMP_SUFFIX).exists());
  }
}
