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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Verifies that {@link FileTestUtil#copy(File, File)} transfers file contents
 * and, more importantly, that it closes both streams on every failure path.
 *
 * <p>Before the streams were wrapped in try-with-resources, a copy that threw
 * partway through skipped the trailing {@code in.close(); out.close();} and
 * leaked the handles until the JVM's cleaner happened to reclaim them.
 */
public class FileTestUtilTest {

  private static final byte[] CONTENT = "the quick brown fox".getBytes();

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void copiesFileContents() throws IOException {
    File src = folder.newFile("src.txt");
    File dst = new File(folder.getRoot(), "dst.txt");
    writeBytes(src, CONTENT);

    FileTestUtil.copy(src, dst);

    assertArrayEquals(CONTENT, readBytes(dst));
  }

  @Test
  public void overwritesExistingDestination() throws IOException {
    File src = folder.newFile("src.txt");
    File dst = folder.newFile("dst.txt");
    writeBytes(src, CONTENT);
    writeBytes(dst, "stale contents that are much longer than the source".getBytes());

    FileTestUtil.copy(src, dst);

    assertArrayEquals(CONTENT, readBytes(dst));
  }

  @Test
  public void copiesContentLargerThanTheTransferBuffer() throws IOException {
    byte[] big = new byte[(1024 * 3) + 17];
    for (int i = 0; i < big.length; i++) {
      big[i] = (byte) i;
    }
    File src = folder.newFile("src.bin");
    File dst = new File(folder.getRoot(), "dst.bin");
    writeBytes(src, big);

    FileTestUtil.copy(src, dst);

    assertArrayEquals(big, readBytes(dst));
  }

  @Test
  public void closesBothStreamsOnSuccess() throws IOException {
    FakeStreams streams = new FakeStreams();

    FileTestUtil.copy(streams, anyFile("src"), anyFile("dst"));

    assertArrayEquals(CONTENT, streams.out.written());
    assertTrue("input stream left open", streams.in.closed);
    assertTrue("output stream left open", streams.out.closed);
  }

  /**
   * The regression this whole exercise is about: the destination could not be
   * opened, so the already-open source stream must still be closed.
   */
  @Test
  public void closesInputWhenOpeningOutputFails() {
    FakeStreams streams = new FakeStreams();
    streams.openOutputFailure = new IOException("dst is a directory");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.openOutputFailure, thrown);
    assertTrue("input stream leaked after output could not be opened", streams.in.closed);
  }

  @Test
  public void closesNothingWhenOpeningInputFails() {
    FakeStreams streams = new FakeStreams();
    streams.openInputFailure = new IOException("src does not exist");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.openInputFailure, thrown);
    assertNull("output stream should never have been opened", streams.out);
  }

  @Test
  public void closesBothStreamsWhenReadFails() {
    FakeStreams streams = new FakeStreams();
    streams.readFailure = new IOException("disk read error");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.readFailure, thrown);
    assertTrue("input stream leaked after read error", streams.in.closed);
    assertTrue("output stream leaked after read error", streams.out.closed);
  }

  @Test
  public void closesBothStreamsWhenWriteFails() {
    FakeStreams streams = new FakeStreams();
    streams.writeFailure = new IOException("no space left on device");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.writeFailure, thrown);
    assertTrue("input stream leaked after write error", streams.in.closed);
    assertTrue("output stream leaked after write error", streams.out.closed);
  }

  /**
   * try-with-resources closes resources in reverse order, so the output stream
   * is already closed by the time the input stream's close() blows up. The old
   * {@code in.close(); out.close();} ordering leaked the output stream here.
   */
  @Test
  public void closesOutputWhenClosingInputFails() {
    FakeStreams streams = new FakeStreams();
    streams.closeInputFailure = new IOException("close failed");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.closeInputFailure, thrown);
    assertTrue("output stream leaked when input close() threw", streams.out.closed);
  }

  /**
   * A failure during the transfer is the one the caller needs to see; a
   * secondary failure from close() must not replace it.
   */
  @Test
  public void reportsTransferFailureAndSuppressesCloseFailure() {
    FakeStreams streams = new FakeStreams();
    streams.readFailure = new IOException("disk read error");
    streams.closeInputFailure = new IOException("close failed");

    IOException thrown = assertCopyFails(streams);

    assertSame(streams.readFailure, thrown);
    assertEquals(1, thrown.getSuppressed().length);
    assertSame(streams.closeInputFailure, thrown.getSuppressed()[0]);
  }

  private IOException assertCopyFails(FakeStreams streams) {
    try {
      FileTestUtil.copy(streams, anyFile("src"), anyFile("dst"));
      fail("expected copy to fail");
      return null; // unreachable
    } catch (IOException e) {
      return e;
    }
  }

  private File anyFile(String name) {
    // never opened: FakeStreams ignores the File and hands back its own streams
    return new File(folder.getRoot(), name);
  }

  private static void writeBytes(File file, byte[] bytes) throws IOException {
    OutputStream out = new FileOutputStream(file);
    try {
      out.write(bytes);
    } finally {
      out.close();
    }
  }

  private static byte[] readBytes(File file) throws IOException {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    InputStream in = new FileInputStream(file);
    try {
      byte[] buf = new byte[512];
      int len;
      while ((len = in.read(buf)) > 0) {
        sink.write(buf, 0, len);
      }
    } finally {
      in.close();
    }
    return sink.toByteArray();
  }

  /**
   * Hands out streams that fail where the test asks them to and remember
   * whether they were closed.
   */
  private static final class FakeStreams implements FileTestUtil.Streams {
    IOException openInputFailure;
    IOException openOutputFailure;
    IOException readFailure;
    IOException writeFailure;
    IOException closeInputFailure;

    TrackingInputStream in;
    TrackingOutputStream out;

    public InputStream openInput(File file) throws IOException {
      if (openInputFailure != null) {
        throw openInputFailure;
      }
      in = new TrackingInputStream(CONTENT, readFailure, closeInputFailure);
      return in;
    }

    public OutputStream openOutput(File file) throws IOException {
      if (openOutputFailure != null) {
        throw openOutputFailure;
      }
      out = new TrackingOutputStream(writeFailure);
      return out;
    }
  }

  private static final class TrackingInputStream extends InputStream {
    private final ByteArrayInputStream delegate;
    private final IOException readFailure;
    private final IOException closeFailure;
    boolean closed;

    TrackingInputStream(byte[] content, IOException readFailure, IOException closeFailure) {
      this.delegate = new ByteArrayInputStream(content);
      this.readFailure = readFailure;
      this.closeFailure = closeFailure;
    }

    @Override
    public int read() throws IOException {
      if (readFailure != null) {
        throw readFailure;
      }
      return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (readFailure != null) {
        throw readFailure;
      }
      return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      if (closeFailure != null) {
        throw closeFailure;
      }
    }
  }

  private static final class TrackingOutputStream extends OutputStream {
    private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
    private final IOException writeFailure;
    boolean closed;

    TrackingOutputStream(IOException writeFailure) {
      this.writeFailure = writeFailure;
    }

    byte[] written() {
      return delegate.toByteArray();
    }

    @Override
    public void write(int b) throws IOException {
      if (writeFailure != null) {
        throw writeFailure;
      }
      delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (writeFailure != null) {
        throw writeFailure;
      }
      delegate.write(b, off, len);
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
