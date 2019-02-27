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
import java.util.List;

import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.status.StatusChecker;

import org.junit.Test;

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

  // helper class used to access protected fields
  class FileAppenderFriend<E> extends FileAppender<E> {
    public void append(E obj) {
      this.subAppend(obj);
    }
  }
}
