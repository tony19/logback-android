/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
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

  // helper class used to access protected fields
  class FileAppenderFriend<E> extends FileAppender<E> {
    public void append(E obj) {
      this.subAppend(obj);
    }
  }
}
