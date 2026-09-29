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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
