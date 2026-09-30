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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

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
  public void gzCompressionOfMissingFileOnlyWarns() {
    File missing = new File(tmpDir.getRoot(), "missing.log");
    File target = new File(tmpDir.getRoot(), "missing.log.gz");

    compressor(CompressionMode.GZ).compress(missing.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.WARN, "The file to compress named [" + missing.getPath() + "] does not exist."));
    assertFalse(target.exists());
    assertFalse(hasStatusStartingWith(Status.INFO, "GZ compressing"));
  }

  @Test
  public void zipCompressionOfMissingFileOnlyWarns() {
    File missing = new File(tmpDir.getRoot(), "missing.log");
    File target = new File(tmpDir.getRoot(), "missing.log.zip");

    compressor(CompressionMode.ZIP).compress(missing.getPath(), target.getPath(), "missing.log");

    assertTrue(hasStatus(Status.WARN, "The file to compress named [" + missing.getPath() + "] does not exist."));
    assertFalse(target.exists());
    assertFalse(hasStatusStartingWith(Status.INFO, "ZIP compressing"));
  }

  @Test
  public void zipCompressionWithoutInnerEntryNameOnlyWarns() throws IOException {
    File target = new File(tmpDir.getRoot(), "app.log.zip");

    compressor(CompressionMode.ZIP).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.WARN, "The innerEntryName parameter cannot be null"));
    assertFalse(target.exists());
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void gzCompressionDoesNotOverwriteExistingArchive() throws IOException {
    File target = tmpDir.newFile("app.log.gz");
    write(target, "previous archive");

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.WARN, "The target compressed file named [" + target.getPath()
        + "] exist already. Aborting file compression."));
    assertEquals("previous archive", read(target));
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void zipCompressionDoesNotOverwriteExistingArchive() throws IOException {
    File target = tmpDir.newFile("app.log.zip");
    write(target, "previous archive");

    compressor(CompressionMode.ZIP).compress(source.getPath(), target.getPath(), "app.log");

    assertTrue(hasStatus(Status.WARN, "The target compressed file named [" + target.getPath() + "] exist already."));
    assertEquals("previous archive", read(target));
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void failureToCreateTargetDirectoryIsReportedAndSourceIsKept() throws IOException {
    // the target's grandparent is a regular file, so its parent can't be created
    File blocker = tmpDir.newFile("blocker");
    File target = new File(new File(blocker, "sub"), "app.log.gz");
    File tmpFile = new File(target.getPath() + Compressor.TMP_SUFFIX);

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.ERROR, "Failed to create parent directories for [" + target.getAbsolutePath() + "]"));
    assertTrue(hasStatusStartingWith(Status.ERROR, "Error occurred while compressing"));
    assertFalse(target.exists());
    // the temp file was never created, so there is nothing to clean up
    assertFalse(tmpFile.exists());
    assertFalse(hasStatusStartingWith(Status.WARN, "Could not delete temporary file"));
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void failureToRenameTempArchiveKeepsSourceAndRemovesTempFile() throws IOException {
    final File target = new File(tmpDir.getRoot(), "app.log.gz");
    File tmpFile = new File(target.getPath() + Compressor.TMP_SUFFIX);
    // right after the existence check of the target, something else occupies
    // its name with a directory, so the final rename of the temp file fails
    onStatus("GZ compressing", () -> assertTrue(target.mkdir()));

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.ERROR, "Failed to rename compressed file [" + tmpFile + "] to [" + target + "]."));
    assertFalse(hasStatusStartingWith(Status.INFO, "Done compressing"));
    assertFalse("temp file must be removed", tmpFile.exists());
    assertFalse("no warning expected for a deletable temp file", hasStatusStartingWith(Status.WARN, "Could not delete"));
    assertTrue(target.isDirectory());
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void undeletableTempFileIsReported() throws IOException {
    File target = new File(tmpDir.getRoot(), "app.log.gz");
    // a (fresh, so not swept) non-empty directory in place of the temp file:
    // it can neither be written nor deleted
    File tmpFile = new File(target.getPath() + Compressor.TMP_SUFFIX);
    assertTrue(tmpFile.mkdir());
    assertTrue(new File(tmpFile, "child").createNewFile());

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatusStartingWith(Status.ERROR, "Error occurred while compressing"));
    assertTrue(hasStatus(Status.WARN, "Could not delete temporary file [" + tmpFile + "]."));
    assertFalse(target.exists());
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void undeletableSourceFileIsReportedAfterSuccessfulCompression() throws IOException {
    final File target = new File(tmpDir.getRoot(), "app.log.gz");
    // once compressed, the source path is replaced with a non-empty directory
    // so that deleting it fails
    onStatus("Done compressing", () -> {
      assertTrue(source.delete());
      assertTrue(source.mkdir());
      assertTrue(new File(source, "child").createNewFile());
    });

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(hasStatus(Status.WARN, "Could not delete [" + source + "]."));
    assertTrue(source.isDirectory());
    assertEquals(CONTENT, gunzip(target));
  }

  @Test
  public void staleTempFileWithUnknownModificationTimeIsKept() throws IOException {
    File unknownAge = tmpDir.newFile("other.log.gz" + Compressor.TMP_SUFFIX);
    // File#lastModified reports 0 when the time is unknown
    assertTrue(unknownAge.setLastModified(0));
    assertEquals(0, unknownAge.lastModified());
    File target = new File(tmpDir.getRoot(), "app.log.gz");

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(unknownAge.exists());
    assertFalse(hasStatusStartingWith(Status.INFO, "Deleted stale temporary file"));
    assertEquals(CONTENT, gunzip(target));
  }

  @Test
  public void staleTempFileThatCannotBeDeletedIsNotReportedAsDeleted() throws IOException {
    File undeletable = tmpDir.newFolder("other.log.zip" + Compressor.TMP_SUFFIX);
    assertTrue(new File(undeletable, "child").createNewFile());
    assertTrue(undeletable.setLastModified(System.currentTimeMillis() - Compressor.STALE_TMP_AGE_MS - 60_000));
    File target = new File(tmpDir.getRoot(), "app.log.gz");

    compressor(CompressionMode.GZ).compress(source.getPath(), target.getPath(), null);

    assertTrue(undeletable.exists());
    assertFalse(hasStatusStartingWith(Status.INFO, "Deleted stale temporary file"));
    assertEquals(CONTENT, gunzip(target));
  }

  @Test
  public void staleTempFileSweepIgnoresTargetWithoutParentDirectory() throws Exception {
    Compressor compressor = compressor(CompressionMode.GZ);
    Method deleteStaleTempFiles = Compressor.class.getDeclaredMethod("deleteStaleTempFiles", File.class);
    deleteStaleTempFiles.setAccessible(true);
    File root = new File(File.separator).getAbsoluteFile();

    // the root directory has no parent to sweep
    deleteStaleTempFiles.invoke(compressor, root);

    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void closeQuietlySwallowsCloseFailure() throws Exception {
    Compressor compressor = compressor(CompressionMode.GZ);
    Method closeQuietly = Compressor.class.getDeclaredMethod("closeQuietly", Closeable.class);
    closeQuietly.setAccessible(true);
    final boolean[] closed = { false };
    Closeable failing = () -> {
      closed[0] = true;
      throw new IOException("close failed");
    };

    closeQuietly.invoke(compressor, failing);

    assertTrue(closed[0]);
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void computeZipEntryNamesEntryAfterArchiveWithoutZipSuffix() {
    Compressor compressor = compressor(CompressionMode.ZIP);

    assertEquals("app-2019-11-04.log", compressor.computeZipEntry(new File("logs", "app-2019-11-04.log.zip")).getName());
    assertEquals("app.log", compressor.computeZipEntry(new File("logs", "app.log")).getName());
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

  private interface IoAction {
    void run() throws IOException;
  }

  /**
   * Runs the action (synchronously, in the compressing thread) when the
   * compressor reports a status whose message starts with the given prefix.
   */
  private void onStatus(final String messagePrefix, final IoAction action) {
    context.getStatusManager().add(new StatusListener() {
      @Override
      public void addStatusEvent(Status status) {
        if (status.getMessage().startsWith(messagePrefix)) {
          try {
            action.run();
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      }
    });
  }

  private boolean hasStatus(int level, String message) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (s.getLevel() == level && message.equals(s.getMessage())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasStatusStartingWith(int level, String prefix) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (s.getLevel() == level && s.getMessage().startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static void write(File file, String content) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(content.getBytes("UTF-8"));
    } finally {
      fos.close();
    }
  }

  private static String read(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    try {
      return readAll(in);
    } finally {
      in.close();
    }
  }

  private static String gunzip(File gzFile) throws IOException {
    InputStream in = new GZIPInputStream(new FileInputStream(gzFile));
    try {
      return readAll(in);
    } finally {
      in.close();
    }
  }

  private static String readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int n;
    while ((n = in.read(buf)) != -1) {
      out.write(buf, 0, n);
    }
    return out.toString("UTF-8");
  }
}
