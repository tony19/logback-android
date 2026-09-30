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
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.recovery.RecoveryCoordinator;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.testUtil.LockProbingOutputStream;
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
    FileAppenderFriend<Object> fa = newFileAppender("prudent-lock", file, encoder);
    fa.probeLocks = true;
    fa.setPrudent(true);
    fa.start();
    // start from a non-interrupted thread
    Thread.interrupted();

    fa.doAppend("hello");
    // reads and clears the flag, so that it cannot leak into later tests
    boolean interruptedAfterAppend = Thread.interrupted();
    fa.stop();

    // the event was encoded before the file was locked, and written while it was
    assertEquals(Collections.singletonList(Boolean.FALSE), encoder.lockHeldDuringEncode);
    assertEquals(Collections.singletonList(Boolean.TRUE), fa.probe.getLockHeldDuringWrite());
    assertEquals("hello\n", readUtf8(file));
    // the thread was not interrupted before the write, so it must not be after it
    assertFalse(interruptedAfterAppend);
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void lazyPrudentAppenderLocksFileItOpensOnFirstAppend() throws Exception {
    File file = new File(tmp.getRoot(), "lazy-prudent.log");
    FileAppenderFriend<Object> fa = newFileAppender("lazy-prudent", file, new Utf8LineEncoder());
    fa.probeLocks = true;
    fa.setPrudent(true);
    fa.setLazy(true);
    fa.start();
    assertFalse(file.exists());

    // the first event opens the file
    fa.doAppend("a");
    assertTrue(fa.getOutputStream() instanceof ResilientFileOutputStream);
    fa.doAppend("b");
    fa.stop();

    // the prudent write locked the file that was opened lazily, from the
    // event that opened it on
    assertEquals(Arrays.asList(Boolean.TRUE, Boolean.TRUE), fa.probe.getLockHeldDuringWrite());
    assertEquals("a\nb\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void nonPrudentModeDoesNotLockFile() throws Exception {
    File file = new File(tmp.getRoot(), "non-prudent.log");
    LockProbingEncoder encoder = new LockProbingEncoder(file);
    FileAppenderFriend<Object> fa = newFileAppender("non-prudent", file, encoder);
    fa.probeLocks = true;
    fa.start();

    fa.doAppend("hello");
    fa.stop();

    assertEquals(Collections.singletonList(Boolean.FALSE), encoder.lockHeldDuringEncode);
    assertEquals(Collections.singletonList(Boolean.FALSE), fa.probe.getLockHeldDuringWrite());
    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
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
    FileAppenderFriend<Object> fa = newFileAppender("shared", file, new Utf8LineEncoder());
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
    FileAppenderFriend<Object> fa = newFileAppender("interrupted", file, new Utf8LineEncoder());
    fa.probeLocks = true;
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
    // the interrupt was cleared while locking (an interrupted thread's
    // FileChannel.lock() closes the channel), so the file was locked and the
    // write went through
    assertEquals(Collections.singletonList(Boolean.TRUE), fa.probe.getLockHeldDuringWrite());
    assertEquals("hello\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentWriteToClosedChannelIsReportedAsIOFailure() throws IOException {
    File file = new File(tmp.getRoot(), "closed.log");
    FileAppenderFriend<Object> fa = newFileAppender("closed", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    ((ResilientFileOutputStream) fa.getOutputStream()).getChannel().close();

    // returns normally: the failure is left to the resilient stream to
    // recover from instead of being thrown at the appender, which would stop it
    fa.doAppend("lost");

    assertTrue(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "IO failure while writing to file");
    checker.asssertContainsException(ClosedChannelException.class);
    checker.assertNoMatch("Appender \\[.*\\] failed to append");
    assertEquals("", readUtf8(file));
    fa.stop();
  }

  @Test
  public void prudentAppenderRecoversOnceItsClosedChannelCanBeReopened() throws Exception {
    File file = new File(tmp.getRoot(), "recovered.log");
    FileAppenderFriend<Object> fa = newFileAppender("recovered", file, new Utf8LineEncoder());
    fa.probeLocks = true;
    fa.setPrudent(true);
    fa.start();
    fa.doAppend("before");
    // what an interrupt while waiting in FileChannel.lock() leaves behind: it
    // closes the channel and the stream it belongs to (see LOGBACK-875)
    ((ResilientFileOutputStream) fa.getOutputStream()).getChannel().close();

    fa.doAppend("lost");
    waitUntilRecoveryIsDue(System.currentTimeMillis());
    // this event makes the resilient stream reopen the file, and is written to it
    fa.doAppend("recovered");
    fa.doAppend("after");
    fa.stop();

    assertEquals("before\nrecovered\nafter\n", readUtf8(file));
    // the reopened file is locked like the original one was
    assertEquals(Arrays.asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), fa.probe.getLockHeldDuringWrite());
    StatusChecker checker = new StatusChecker(context);
    checker.asssertContainsException(ClosedChannelException.class);
    checker.assertContainsMatch(Status.INFO, "Attempting to recover from IO failure on file");
    checker.assertNoMatch("Failed to open");
    // recovery is reported once an event was written to the reopened file,
    // not when the broken stream is closed to reopen it
    int attempting = indexOfFirstStatus("Attempting to recover from IO failure on file");
    int recovered = indexOfFirstStatus("Recovered from IO failure on file");
    assertTrue("recovery reported before it was attempted", attempting < recovered);
  }

  @Test
  public void prudentAppenderKeepsBackingOffWhileItsFileCannotBeReopened() throws Exception {
    File dir = tmp.newFolder("gone");
    File file = new File(dir, "gone.log");
    FileAppenderFriend<Object> fa = newFileAppender("gone", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    fa.doAppend("before");
    // the channel is closed, as in the test above, and the file cannot be
    // reopened: its directory is gone (e.g. unmounted storage)
    ((ResilientFileOutputStream) fa.getOutputStream()).getChannel().close();
    assertTrue(file.delete());
    assertTrue(dir.delete());

    fa.doAppend("lost");
    waitUntilRecoveryIsDue(System.currentTimeMillis());
    // makes the resilient stream try to reopen the file, which fails
    fa.doAppend("lost too");

    assertTrue(fa.isStarted());
    StatusChecker checker = new StatusChecker(context);
    assertEquals(1, checker.matchCount("Attempting to recover from IO failure on file"));
    checker.assertContainsMatch(Status.ERROR, "Failed to open file");
    // the stream is still in error, and backs off further before the next
    // attempt: reporting it recovered would restart its back-off and its
    // status count, i.e. a reopen attempt and four statuses for every event
    // logged 20 ms after the last one
    checker.assertNoMatch("Recovered from IO failure");
    checker.assertNoMatch("Appender \\[.*\\] failed to append");
    fa.stop();
  }

  @Test
  public void prudentWriteDoesNotReleaseLockInvalidatedDuringWriteAndRecovers() throws Exception {
    File file = new File(tmp.getRoot(), "invalidated.log");
    FileAppenderFriend<Object> fa = newFileAppender("invalidated", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    fa.setOutputStream(new ChannelClosingOutputStream(file, context));

    // returns normally, without trying to release the lock that closing its
    // channel invalidated: that would fail with ClosedChannelException
    fa.doAppend("lost");
    waitUntilRecoveryIsDue(System.currentTimeMillis());
    fa.doAppend("recovered");
    fa.stop();

    assertEquals("recovered\n", readUtf8(file));
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "IO failure while writing to file");
    checker.assertContainsMatch(Status.INFO, "Attempting to recover from IO failure on file");
    checker.assertNoMatch("failed to release lock");
    checker.assertNoMatch("Appender \\[.*\\] failed to append");
  }

  @Test
  public void prudentWriteReportsLockReleaseFailureAndKeepsWriting() throws IOException {
    File file = new File(tmp.getRoot(), "release-failure.log");
    FileAppenderFriend<Object> fa = newFileAppender("release-failure", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    fa.setOutputStream(new LockReleaseFailingOutputStream(file, context));

    fa.doAppend("a");
    fa.doAppend("b");

    // the events were written before the lock was to be released, and the
    // appender was not stopped over failing to release it
    assertTrue(fa.isStarted());
    assertEquals("a\nb\n", readUtf8(file));
    StatusChecker checker = new StatusChecker(context);
    assertEquals(2, checker.matchCount("failed to release lock"));
    checker.asssertContainsException(IOException.class);
    checker.assertNoMatch("IO failure in appender");
    fa.stop();
  }

  @Test
  public void prudentWriteDoesNotLockFileForEventEncodedToNothing() throws IOException {
    File file = new File(tmp.getRoot(), "encoded-to-nothing.log");
    Utf8LineEncoder encoder = new Utf8LineEncoder() {
      @Override
      public byte[] encode(Object event) {
        return "null".equals(event) ? null : new byte[0];
      }
    };
    FileAppenderFriend<Object> fa = newFileAppender("encoded-to-nothing", file, encoder);
    fa.setPrudent(true);
    fa.start();
    // any lock taken through this stream's channel fails to be released
    fa.setOutputStream(new LockReleaseFailingOutputStream(file, context));

    fa.doAppend("null");
    fa.doAppend("empty");
    fa.stop();

    assertEquals("", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentWriteIsSkippedWhenStreamHasNoChannel() throws IOException {
    File file = new File(tmp.getRoot(), "no-channel.log");
    FileAppenderFriend<Object> fa = newFileAppender("no-channel", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    fa.setOutputStream(new ChannelLessResilientFileOutputStream(file));

    fa.doAppend("dropped");
    fa.stop();

    assertEquals("", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void prudentAppenderWritesToStreamWithoutFileAsNonPrudentOneDoes() {
    File file = new File(tmp.getRoot(), "not-a-file-stream.log");
    FileAppenderFriend<Object> fa = newFileAppender("not-a-file-stream", file, new Utf8LineEncoder());
    fa.setPrudent(true);
    fa.start();
    FlushCountingOutputStream out = new FlushCountingOutputStream();
    fa.setOutputStream(out);

    fa.doAppend("a");
    fa.doAppend("b");
    fa.stop();

    // there is no file to lock: written and flushed as without prudent mode
    assertEquals("a\nb\n", new String(out.toByteArray(), StandardCharsets.UTF_8));
    assertEquals(2, out.flushCount);
    new StatusChecker(context).assertIsErrorFree();
  }

  @Test
  public void lazyPrudentAppenderDiscardsEventsQuietlyAfterFileNameCollision() throws IOException {
    File file = new File(tmp.getRoot(), "lazy-prudent-collision.log");
    FileAppender<Object> first = newFileAppender("first", file, new Utf8LineEncoder());
    first.start();
    FileAppender<Object> lazy = newFileAppender("lazy", file, new Utf8LineEncoder());
    lazy.setPrudent(true);
    lazy.setLazy(true);
    lazy.start();

    lazy.doAppend("a");
    lazy.doAppend("b");

    // the stream the appender writes to stays the NOPOutputStream: its events
    // are discarded, as without prudent mode, without failing to be appended
    assertTrue(lazy.isStarted());
    assertTrue(lazy.getOutputStream() instanceof NOPOutputStream);
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR,
        "Collisions detected with FileAppender/RollingAppender instances defined earlier. Aborting.");
    checker.assertNoMatch("Appender \\[.*\\] failed to append");
    assertEquals("", readUtf8(file));
    first.stop();
    lazy.stop();
  }

  @Test
  public void prudentAppenderKeepsEveryEventLoggedConcurrently() throws Exception {
    File file = new File(tmp.getRoot(), "prudent-concurrent.log");
    final int threadCount = 2;
    final int eventsPerThread = 200;
    // the threads' first events are encoded at the same time, i.e. each while
    // the other thread's append is in progress. Routing appends through the
    // prudent writeOut(), which locks the file without holding the appender's
    // lock, made the second thread's FileChannel.lock() throw
    // OverlappingFileLockException there, and its events were dropped.
    final FileAppender<Object> fa = newFileAppender("prudent-concurrent", file,
        new RendezvousEncoder(threadCount));
    fa.setPrudent(true);
    fa.start();

    final CountDownLatch startGate = new CountDownLatch(1);
    final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());
    List<Thread> threads = new ArrayList<Thread>();
    List<String> expected = new ArrayList<String>();
    for (int t = 0; t < threadCount; t++) {
      final String prefix = "thread" + t + "-event";
      for (int i = 0; i < eventsPerThread; i++) {
        expected.add(prefix + i);
      }
      Thread thread = new Thread(() -> {
        try {
          startGate.await();
          for (int i = 0; i < eventsPerThread; i++) {
            fa.doAppend(prefix + i);
          }
        } catch (Throwable e) {
          failures.add(e);
        }
      }, "prudent-writer-" + t);
      thread.start();
      threads.add(thread);
    }

    startGate.countDown();
    for (Thread thread : threads) {
      thread.join(TimeUnit.SECONDS.toMillis(30));
      assertFalse(thread.getName() + " did not finish", thread.isAlive());
    }
    fa.stop();

    assertEquals(Collections.<Throwable>emptyList(), failures);
    List<String> lines = new ArrayList<String>(Arrays.asList(readUtf8(file).split("\n")));
    assertEquals("events in the file", expected.size(), lines.size());
    Collections.sort(expected);
    Collections.sort(lines);
    assertEquals(expected, lines);
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

    // the file was neither reopened nor checked again for collisions (which
    // would now collide with this appender's own entry) for the second event
    assertSame(opened, fa.getOutputStream());
    assertEquals("a\nb\n", readUtf8(file));
    new StatusChecker(context).assertIsErrorFree();
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

  private FileAppenderFriend<Object> newFileAppender(String name, File file, EncoderBase<Object> encoder) {
    FileAppenderFriend<Object> fa = new FileAppenderFriend<Object>();
    fa.setContext(context);
    fa.setName(name);
    fa.setEncoder(encoder);
    fa.setFile(file.getAbsolutePath());
    return fa;
  }

  private int indexOfFirstStatus(String messagePrefix) {
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    for (int i = 0; i < statuses.size(); i++) {
      if (statuses.get(i).getMessage().startsWith(messagePrefix)) {
        return i;
      }
    }
    fail("no status starting with: " + messagePrefix);
    return -1;
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
   * Waits until a resilient stream that failed by {@code failedBy} may try to
   * recover: its RecoveryCoordinator allows the first attempt once the clock
   * is past the time of the failure plus
   * {@link RecoveryCoordinator#BACKOFF_COEFFICIENT_MIN}. A slow runner only
   * makes the wait longer; it cannot make the attempt come too soon.
   */
  static void waitUntilRecoveryIsDue(long failedBy) throws InterruptedException {
    while (System.currentTimeMillis() <= failedBy + RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN) {
      Thread.sleep(1);
    }
  }

  /**
   * Closes its file channel on its first write, i.e. while a prudent
   * appender holds the lock it took through that channel, which invalidates
   * the lock.
   */
  static class ChannelClosingOutputStream extends ResilientFileOutputStream {
    private boolean channelClosed;

    ChannelClosingOutputStream(File file, Context context) throws FileNotFoundException {
      super(file, true, FileAppender.DEFAULT_BUFFER_SIZE);
      setContext(context);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if (!channelClosed) {
        channelClosed = true;
        try {
          getChannel().close();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
      super.write(b, off, len);
    }
  }

  /**
   * Writes to its file, but its channel hands out file locks that fail to be
   * released.
   */
  static class LockReleaseFailingOutputStream extends ResilientFileOutputStream {
    LockReleaseFailingOutputStream(File file, Context context) throws FileNotFoundException {
      super(file, true, FileAppender.DEFAULT_BUFFER_SIZE);
      setContext(context);
    }

    @Override
    public FileChannel getChannel() {
      return new LockReleaseFailingChannel(super.getChannel());
    }
  }

  /**
   * Delegates what a prudent appender uses, apart from locking, to the given
   * channel; its locks are not real, and fail to be released.
   */
  static class LockReleaseFailingChannel extends FileChannel {
    private final FileChannel channel;

    LockReleaseFailingChannel(FileChannel channel) {
      this.channel = channel;
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) {
      return new FileLock(this, position, size, shared) {
        @Override
        public boolean isValid() {
          return true;
        }

        @Override
        public void release() throws IOException {
          throw new IOException("release failed");
        }
      };
    }

    @Override
    public long position() throws IOException {
      return channel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
      channel.position(newPosition);
      return this;
    }

    @Override
    public long size() throws IOException {
      return channel.size();
    }

    @Override
    protected void implCloseChannel() throws IOException {
      channel.close();
    }

    // not used by the appender

    @Override
    public int read(ByteBuffer dst) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int write(ByteBuffer src) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileChannel truncate(long size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void force(boolean metaData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int read(ByteBuffer dst, long position) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int write(ByteBuffer src, long position) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Keeps what is written, and counts flushes.
   */
  static class FlushCountingOutputStream extends ByteArrayOutputStream {
    int flushCount;

    @Override
    public void flush() {
      flushCount++;
    }
  }

  /**
   * Holds each event in encode() until as many events as there are parties
   * are being encoded at the same time, so that the first events of that many
   * threads are written concurrently; later events are not held. The wait is
   * bounded, and the events are encoded when it ends either way, so the
   * outcome does not depend on how long it takes.
   */
  static class RendezvousEncoder extends Utf8LineEncoder {
    final CountDownLatch encoding;

    RendezvousEncoder(int parties) {
      encoding = new CountDownLatch(parties);
    }

    @Override
    public byte[] encode(Object event) {
      encoding.countDown();
      try {
        encoding.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
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
   * while the event was being encoded.
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

  // helper class used to access protected members
  class FileAppenderFriend<E> extends FileAppender<E> {
    // when set, the file is written through the probe, which it opens instead
    boolean probeLocks;
    LockProbingOutputStream probe;

    public void append(E obj) {
      this.subAppend(obj);
    }

    @Override
    protected boolean openFile(String filename) throws IOException {
      boolean opened = super.openFile(filename);
      if (probeLocks) {
        probe = new LockProbingOutputStream(((ResilientFileOutputStream) getOutputStream()).getFile());
        probe.setContext(getContext());
        setOutputStream(probe);
      }
      return opened;
    }
  }
}
