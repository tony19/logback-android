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
package ch.qos.logback.core.appender;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.status.StatusChecker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.NOPOutputStream;
import ch.qos.logback.core.encoder.DummyEncoder;
import ch.qos.logback.core.encoder.NopEncoder;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;

public class FileAppenderTest extends AbstractAppenderTest<Object> {

  private int diff = RandomUtil.getPositiveInt();

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  protected Appender<Object> getAppender() {
    return new FileAppender<Object>();
  }

  protected Appender<Object> getConfiguredAppender() {
    FileAppender<Object> appender = new FileAppender<Object>();
    appender.setEncoder(new NopEncoder<Object>());
    appender.setFile(CoreTestConstants.OUTPUT_DIR_PREFIX+"temp.log");
    appender.setName("test");
    appender.setContext(context);
    appender.start();
    return appender;
  }

  @Test
  public void smoke() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + "/fat-smoke.log";

    FileAppender<Object> appender = new FileAppender<Object>();
    appender.setEncoder(new DummyEncoder<Object>());
    appender.setAppend(false);
    appender.setFile(filename);
    appender.setName("smoke");
    appender.setContext(context);
    appender.start();
    appender.doAppend(new Object());
    appender.stop();

    File file = new File(filename);
    assertTrue(file.exists());
    assertTrue("failed to delete " + file.getAbsolutePath(), file.delete());
  }

  @Test
  public void testCreateParentFolders() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + "/fat-testCreateParentFolders-" + diff
        + "/testCreateParentFolders.txt";
    File file = new File(filename);
    assertFalse(file.getParentFile().exists());
    assertFalse(file.exists());

    FileAppender<Object> appender = new FileAppender<Object>();
    appender.setEncoder(new DummyEncoder<Object>());
    appender.setAppend(false);
    appender.setFile(filename);
    appender.setName("testCreateParentFolders");
    appender.setContext(context);
    appender.start();
    appender.doAppend(new Object());
    appender.stop();
    assertTrue(file.getParentFile().exists());
    assertTrue(file.exists());

    // cleanup
    assertTrue("failed to delete " + file.getAbsolutePath(), file.delete());
    File parent = file.getParentFile();
    assertTrue("failed to delete " + parent.getAbsolutePath(), parent.delete());
  }

  @Test
  public void testPrudentModeLogicalImplications() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "fat-testPrudentModeLogicalImplications.txt";
    File file = new File(filename);
    FileAppender<Object> appender = new FileAppender<Object>();
    appender.setEncoder(new DummyEncoder<Object>());
    appender.setFile(filename);
    appender.setName("testPrudentModeLogicalImplications");
    appender.setContext(context);

    appender.setAppend(false);
    appender.setPrudent(true);
    appender.start();

    assertTrue(appender.isAppend());

    StatusManager sm = context.getStatusManager();
    //StatusPrinter.print(context);
    StatusChecker statusChecker = new StatusChecker(context);
    assertEquals(Status.WARN, statusChecker.getHighestLevel(0));
    List<Status> statusList = sm.getCopyOfStatusList();
    assertTrue("Expecting status list size to be 2 or larger, but was "
        + statusList.size(), statusList.size() >= 2);
    String msg1 = statusList.get(1).getMessage();

    assertTrue("Got message [" + msg1 + "]", msg1
        .startsWith("Setting \"Append\" property"));

    appender.doAppend(new Object());
    appender.stop();
    assertTrue(file.exists());
    assertTrue("failed to delete " + file.getAbsolutePath(), file.delete());
  }

  private FileAppenderFriend<Object> getFileAppender(String filename) {
    FileAppenderFriend<Object> fa = new FileAppenderFriend<Object>();
    fa.setEncoder(new DummyEncoder<Object>());
    fa.setFile(filename);
    fa.setName("testPrudentMode");
    fa.setContext(context);

    fa.setAppend(false);
    fa.setPrudent(true);
    return fa;
  }

  @Test
  public void unlazyAppenderOpensFileAtStart() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "testing.txt";
    File file = new File(filename);
    if (file.exists()) file.delete();
    FileAppender<Object> fa = getFileAppender(filename);
    fa.setLazy(false);

    assertNull("stream is not null", fa.getOutputStream());
    fa.start();
    assertTrue("expected ResilientFileOutputStream; actual " + fa.getOutputStream().getClass().getSimpleName(), fa.getOutputStream() instanceof ResilientFileOutputStream);
    assertTrue("file does not exist", file.exists());
  }

  @Test
  public void lazyAppenderDoesNotOpenFileAtStart() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "testing.txt";
    File file = new File(filename);
    if (file.exists()) file.delete();
    FileAppender<Object> fa = getFileAppender(filename);
    fa.setLazy(true);

    assertNull("stream is not null", fa.getOutputStream());
    fa.start();
    assertTrue("expected NOPOutputStream; actual " + fa.getOutputStream().getClass().getSimpleName(), fa.getOutputStream() instanceof NOPOutputStream);
    assertFalse("file does not exist", file.exists());
  }

  @Test
  public void lazyAppenderOpensFileOnAppend() {
    String filename = CoreTestConstants.OUTPUT_DIR_PREFIX + diff + "testing.txt";
    File file = new File(filename);
    if (file.exists()) file.delete();
    FileAppenderFriend<Object> fa = getFileAppender(filename);
    fa.setLazy(true);

    fa.start();
    assertTrue("expected NOPOutputStream; actual " + fa.getOutputStream().getClass().getSimpleName(), fa.getOutputStream() instanceof NOPOutputStream);
    fa.append(new Object());
    assertTrue("expected ResilientFileOutputStream; actual " + fa.getOutputStream().getClass().getSimpleName(), fa.getOutputStream() instanceof ResilientFileOutputStream);
    assertTrue("file does not exist", file.exists());
  }

  @Test
  public void fileNameCollision() {
    String fileName = CoreTestConstants.OUTPUT_DIR_PREFIX + diff+ "fileNameCollision";

    FileAppender<Object> appender0 = new FileAppender<Object>();
    appender0.setName("FA0");
    appender0.setFile(fileName);
    appender0.setContext(context);
    appender0.setEncoder(new DummyEncoder<Object>());
    appender0.start();
    assertTrue(appender0.isStarted());

    FileAppender<Object> appender1 = new FileAppender<Object>();
    appender1.setName("FA1");
    appender1.setFile(fileName);
    appender1.setContext(context);
    appender1.setEncoder(new DummyEncoder<Object>());
    appender1.start();

    assertFalse(appender1.isStarted());

    StatusPrinter.print(context);
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "'File' option has the same value");
  }

  @Test
  public void prudentModeHoldsFileLockWhileEventIsWritten() throws Exception {
    File file = new File(tmp.getRoot(), "prudent-lock.log");
    LockProbingEncoder encoder = new LockProbingEncoder(file);
    FileAppender<Object> fa = newFileAppender("prudent-lock", file, encoder);
    fa.setPrudent(true);
    fa.start();

    fa.doAppend("hello");
    fa.stop();

    assertEquals(Collections.singletonList(Boolean.TRUE), encoder.lockHeldDuringEncode);
    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void nonPrudentModeDoesNotLockFile() throws Exception {
    File file = new File(tmp.getRoot(), "non-prudent.log");
    LockProbingEncoder encoder = new LockProbingEncoder(file);
    FileAppender<Object> fa = newFileAppender("non-prudent", file, encoder);
    fa.start();

    fa.doAppend("hello");
    fa.stop();

    assertEquals(Collections.singletonList(Boolean.FALSE), encoder.lockHeldDuringEncode);
    assertEquals("hello\n", readUtf8(file));
  }

  @Test
  public void startWithoutFileReportsErrorAndDoesNotStart() {
    FileAppender<Object> fa = new FileAppender<Object>();
    fa.setContext(context);
    fa.setName("nofile");
    fa.setEncoder(new Utf8LineEncoder());

    fa.start();

    assertFalse(fa.isStarted());
    assertNull(fa.getOutputStream());
    new StatusChecker(context).assertContainsMatch(Status.ERROR,
        "\"File\" property not set for appender named \\[nofile\\]");
  }

  @Test
  public void startReportsErrorWhenFileCannotBeOpened() {
    // a directory cannot be opened as a log file
    File dir = tmp.getRoot();
    FileAppender<Object> fa = newFileAppender("dir", dir, new Utf8LineEncoder());

    fa.start();

    assertFalse(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR,
        Pattern.quote("openFile(" + dir.getAbsolutePath() + ",true) failed"));
    checker.asssertContainsException(FileNotFoundException.class);
  }

  @Test
  public void startReportsErrorWhenParentDirectoryCannotBeCreated() throws IOException {
    // the parent directory would have to be created under a regular file
    File blocker = tmp.newFile("blocker");
    File file = new File(new File(blocker, "sub"), "app.log");
    FileAppender<Object> fa = newFileAppender("noparent", file, new Utf8LineEncoder());

    fa.start();

    assertFalse(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR,
        Pattern.quote("Failed to create parent directories for [" + file.getAbsolutePath() + "]"));
    checker.assertContainsMatch(Status.ERROR,
        Pattern.quote("openFile(" + file.getAbsolutePath() + ",true) failed"));
  }

  @Test
  public void appenderWorksWithoutFilenameCollisionMap() throws IOException {
    ContextBase contextWithoutMap = new ContextBase();
    contextWithoutMap.removeObject(CoreConstants.FA_FILENAME_COLLISION_MAP);
    File file = new File(tmp.getRoot(), "nomap.log");
    FileAppender<Object> fa = new FileAppender<Object>();
    fa.setContext(contextWithoutMap);
    fa.setName("nomap");
    fa.setEncoder(new Utf8LineEncoder());
    fa.setFile(file.getAbsolutePath());

    fa.start();
    fa.doAppend("hello");
    fa.stop();

    assertFalse(fa.isStarted());
    assertEquals("hello\n", readUtf8(file));
    assertNull(contextWithoutMap.getObject(CoreConstants.FA_FILENAME_COLLISION_MAP));
    new StatusChecker(contextWithoutMap).assertIsErrorFree();
  }

  @Test
  public void stoppedAppenderReleasesItsFileForLaterAppenders() {
    File file = new File(tmp.getRoot(), "released.log");
    FileAppender<Object> first = newFileAppender("first", file, new Utf8LineEncoder());
    first.start();
    first.stop();

    FileAppender<Object> second = newFileAppender("second", file, new Utf8LineEncoder());
    second.start();

    assertTrue(second.isStarted());
    second.stop();
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void unnamedAppenderCanBeStartedAndStopped() throws IOException {
    File file = new File(tmp.getRoot(), "unnamed.log");
    FileAppender<Object> fa = newFileAppender(null, file, new Utf8LineEncoder());

    fa.start();
    fa.doAppend("hello");
    fa.stop();

    assertFalse(fa.isStarted());
    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void lazyAndSyncOnFlushAreOffByDefault() {
    FileAppender<Object> fa = new FileAppender<Object>();
    assertFalse(fa.getLazy());
    assertFalse(fa.isSyncOnFlush());

    fa.setLazy(true);
    fa.setSyncOnFlush(true);

    assertTrue(fa.getLazy());
    assertTrue(fa.isSyncOnFlush());
  }

  @Test
  public void eventsStayInDefaultBufferWithoutImmediateFlush() throws IOException {
    File file = new File(tmp.getRoot(), "buffered.log");
    FileAppender<Object> fa = newFileAppender("buffered", file, new Utf8LineEncoder());
    fa.setImmediateFlush(false);
    fa.start();

    fa.doAppend("hello");

    assertEquals("", readUtf8(file));
    fa.stop();
    assertEquals("hello\n", readUtf8(file));
  }

  @Test
  public void eventsLargerThanBufferSizeReachFileWithoutFlush() throws IOException {
    File file = new File(tmp.getRoot(), "small-buffer.log");
    FileAppender<Object> fa = newFileAppender("small-buffer", file, new Utf8LineEncoder());
    fa.setImmediateFlush(false);
    fa.setBufferSize(new FileSize(4));
    fa.start();

    fa.doAppend("hello");

    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertContainsMatch(Status.INFO, "Setting bufferSize to \\[4 Bytes\\]");
    fa.stop();
  }

  @Test
  public void prudentWriteSeeksPastDataAppendedByAnotherWriter() throws IOException {
    File file = new File(tmp.getRoot(), "shared.log");
    FileAppender<Object> fa = newFileAppender("shared", file, new Utf8LineEncoder());
    // not in append mode, so the stream's position does not follow the end of file
    fa.setAppend(false);
    fa.start();
    fa.doAppend("a");
    appendUtf8(file, "other\n");
    fa.setPrudent(true);

    fa.doAppend("b");
    fa.stop();

    assertEquals("a\nother\nb\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentWriteRestoresInterruptFlag() throws IOException {
    File file = new File(tmp.getRoot(), "interrupted.log");
    FileAppender<Object> fa = newFileAppender("interrupted", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();

    boolean interruptedAfterAppend;
    Thread.currentThread().interrupt();
    try {
      fa.doAppend("hello");
    } finally {
      // also clears the flag for later tests
      interruptedAfterAppend = Thread.interrupted();
    }
    fa.stop();

    assertTrue(interruptedAfterAppend);
    // the interrupt was cleared while locking, so the write went through
    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentWriteToClosedChannelIsReportedAsIOFailure() throws IOException {
    File file = new File(tmp.getRoot(), "closed.log");
    FileAppender<Object> fa = newFileAppender("closed", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    ((ResilientFileOutputStream) fa.getOutputStream()).getChannel().close();

    fa.doAppend("lost");

    // the failure is left to the resilient stream to recover from
    assertTrue(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "IO failure while writing to file");
    checker.asssertContainsException(ClosedChannelException.class);
    checker.assertNoMatch("failed to append");
    assertEquals("", readUtf8(file));
    fa.stop();
  }

  @Test
  public void prudentWriteDoesNotReleaseLockInvalidatedDuringWrite() throws IOException {
    File file = new File(tmp.getRoot(), "invalidated.log");
    ChannelClosingEncoder encoder = new ChannelClosingEncoder();
    FileAppender<Object> fa = newFileAppender("invalidated", file, encoder);
    encoder.appender = fa;
    fa.setPrudent(true);
    fa.start();

    fa.doAppend("lost");

    // releasing the invalidated lock would have thrown and stopped the appender
    assertTrue(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertNoMatch("IO failure in appender");
    checker.assertContainsMatch(Status.ERROR, "IO failure while writing to file");
    fa.stop();
  }

  @Test
  public void prudentWriteIsSkippedWhenStreamHasNoChannel() throws IOException {
    File file = new File(tmp.getRoot(), "no-channel.log");
    FileAppender<Object> fa = newFileAppender("no-channel", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    fa.setOutputStream(new ChannelLessResilientFileOutputStream(file));

    fa.doAppend("dropped");
    fa.stop();

    assertEquals("", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void lazyAppenderOpensFileOnlyOnce() throws IOException {
    File file = new File(tmp.getRoot(), "lazy-once.log");
    FileAppender<Object> fa = newFileAppender("lazy-once", file, new Utf8LineEncoder());
    fa.setLazy(true);
    fa.start();

    fa.doAppend("a");
    OutputStream opened = fa.getOutputStream();
    fa.doAppend("b");

    // the file was not reopened for the second event
    assertSame(opened, fa.getOutputStream());
    assertEquals("a\nb\n", readUtf8(file));
    fa.stop();
  }

  @Test
  public void lazyAppenderReportsCollisionOnFirstAppend() throws IOException {
    File file = new File(tmp.getRoot(), "lazy-collision.log");
    FileAppender<Object> first = newFileAppender("first", file, new Utf8LineEncoder());
    first.start();
    FileAppender<Object> lazy = newFileAppender("lazy", file, new Utf8LineEncoder());
    lazy.setLazy(true);
    lazy.start();
    assertTrue(lazy.isStarted());

    lazy.doAppend("from lazy");

    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR,
        "Collisions detected with FileAppender/RollingAppender instances defined earlier. Aborting.");
    checker.assertContainsMatch(Status.ERROR, "'File' option has the same value");
    assertTrue(lazy.getOutputStream() instanceof NOPOutputStream);
    assertEquals("", readUtf8(file));
    first.stop();
    lazy.stop();
  }

  @Test
  public void lazyAppenderStopsWhenFileCannotBeOpened() {
    File dir = tmp.getRoot();
    FileAppender<Object> fa = newFileAppender("lazy-dir", dir, new Utf8LineEncoder());
    fa.setLazy(true);
    fa.start();
    assertTrue(fa.isStarted());

    fa.doAppend("lost");

    assertFalse(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR,
        Pattern.quote("openFile(" + dir.getAbsolutePath() + ",true) failed"));
    checker.asssertContainsException(FileNotFoundException.class);
  }

  private FileAppender<Object> newFileAppender(String name, File file, EncoderBase<Object> encoder) {
    FileAppender<Object> fa = new FileAppender<Object>();
    fa.setContext(context);
    fa.setName(name);
    fa.setEncoder(encoder);
    fa.setFile(file.getAbsolutePath());
    return fa;
  }

  static String readUtf8(File file) throws IOException {
    RandomAccessFile raf = new RandomAccessFile(file, "r");
    try {
      byte[] bytes = new byte[(int) raf.length()];
      raf.readFully(bytes);
      return new String(bytes, StandardCharsets.UTF_8);
    } finally {
      raf.close();
    }
  }

  /**
   * Encodes each event as its string form in UTF-8 followed by a newline.
   */
  static class Utf8LineEncoder extends EncoderBase<Object> {
    @Override
    public byte[] headerBytes() {
      return null;
    }

    @Override
    public byte[] encode(Object event) {
      return (event + "\n").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] footerBytes() {
      return null;
    }
  }

  static void appendUtf8(File file, String text) throws IOException {
    FileOutputStream out = new FileOutputStream(file, true);
    try {
      out.write(text.getBytes(StandardCharsets.UTF_8));
    } finally {
      out.close();
    }
  }

  /**
   * Closes the appender's file channel while encoding, i.e. after the prudent
   * write locked the file, which invalidates that lock.
   */
  static class ChannelClosingEncoder extends Utf8LineEncoder {
    FileAppender<Object> appender;

    @Override
    public byte[] encode(Object event) {
      try {
        ((ResilientFileOutputStream) appender.getOutputStream()).getChannel().close();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return super.encode(event);
    }
  }

  /**
   * A ResilientFileOutputStream whose underlying stream is gone, so it has no
   * file channel.
   */
  static class ChannelLessResilientFileOutputStream extends ResilientFileOutputStream {
    ChannelLessResilientFileOutputStream(File file) throws IOException {
      super(file, true, FileAppender.DEFAULT_BUFFER_SIZE);
      close();
      os = null;
    }
  }

  /**
   * Records, for each encoded event, whether this JVM held a lock on the file
   * while the event was being encoded (i.e. written).
   */
  static class LockProbingEncoder extends Utf8LineEncoder {
    final File file;
    final List<Boolean> lockHeldDuringEncode = new ArrayList<Boolean>();

    LockProbingEncoder(File file) {
      this.file = file;
    }

    @Override
    public byte[] encode(Object event) {
      lockHeldDuringEncode.add(isLockedByThisJvm());
      return super.encode(event);
    }

    private boolean isLockedByThisJvm() {
      try {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
          FileLock lock = raf.getChannel().tryLock();
          if (lock != null) {
            lock.release();
          }
          return false;
        } catch (OverlappingFileLockException e) {
          return true;
        } finally {
          raf.close();
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  // helper class used to access protected fields
  class FileAppenderFriend<E> extends FileAppender<E> {
    public void append(E obj) {
      this.subAppend(obj);
    }
  }
}
