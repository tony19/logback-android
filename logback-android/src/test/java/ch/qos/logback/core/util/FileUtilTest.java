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
package ch.qos.logback.core.util;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.android.AndroidContextUtil;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.status.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;

public class FileUtilTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  FileUtil fileUtil = new FileUtil(context);
  List<File> cleanupList = new ArrayList<File>();
  // test-output folder is not always clean
  int diff =  new Random().nextInt(10000);

  @Before
  public void setUp() throws Exception {
    
  }

  @After
  public void tearDown() throws Exception {
    for(File f: cleanupList) {
      f.delete();
    }
  }

  @Test
  public void checkParentCreationInquiryAndSubsequentCreation() {
    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/testing.txt");
    // these will be deleted later
    cleanupList.add(file);
    cleanupList.add(file.getParentFile());

    assertFalse(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
    assertTrue(file.getParentFile().exists());
  }
  
  @Test
  public void checkDeeperParentCreationInquiryAndSubsequentCreation() {

    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/bla/testing.txt");
    // these will be deleted later
    cleanupList.add(file);
    cleanupList.add(file.getParentFile());
    cleanupList.add(file.getParentFile().getParentFile());

    assertFalse(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
    assertTrue(file.getParentFile().exists());
  }

  @Test
  public void basicCopyingWorks() throws IOException {
    String dir = CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff;

    File dirFile  = new File(dir);
    dirFile.mkdir();

    String src = CoreTestConstants.TEST_INPUT_PREFIX + "compress1.copy";
    String target = CoreTestConstants.OUTPUT_DIR_PREFIX+"/fu"+diff+"/copyingWorks.txt";

    fileUtil.copy(src, target);
    Compare.compare(src, target);
  }

  @Test
  public void createParentDirIgnoresExistingDir() {
    String target = CoreTestConstants.OUTPUT_DIR_PREFIX + "/fu" + diff + "/testing.txt";
    File file = new File(target);
    cleanupList.add(file);
    file.mkdirs();
    assertTrue(file.getParentFile().exists());
    assertTrue(FileUtil.createMissingParentDirectories(file));
  }

  @Test
  public void createParentDirAcceptsNoParentSpecified() {
    File file = new File("testing.txt");
    assertTrue(FileUtil.createMissingParentDirectories(file));
  }

  // Issue #228: on Android 11+, mkdirs() fails for paths beneath the
  // app-specific external directories until the platform creates the base
  // dirs upon the app's request. Verify that when the first mkdirs() fails,
  // the base directories are requested from AndroidContextUtil and mkdirs()
  // is retried.
  @Test
  public void createParentDirRetriesAfterCreatingAppExternalStorageDirs() throws IOException {
    // a regular file blocking the parent chain makes the first mkdirs()
    // fail, standing in for the OS rejecting creation of Android/data dirs
    final File blocker = new File(CoreTestConstants.OUTPUT_DIR_PREFIX + "/fu" + diff + "/blocker");
    File file = new File(blocker, "logs/testing.txt");
    cleanupList.add(file);
    cleanupList.add(file.getParentFile());
    cleanupList.add(blocker);
    cleanupList.add(blocker.getParentFile());

    blocker.getParentFile().mkdirs();
    assertTrue(blocker.createNewFile());

    AndroidContextUtil platform = new AndroidContextUtil(null) {
      @Override
      public void createAppExternalStorageDirs() {
        // simulate the platform creating the requested base directories
        blocker.delete();
        blocker.mkdirs();
      }
    };

    assertTrue(FileUtil.createMissingParentDirectories(file, platform));
    assertTrue(file.getParentFile().exists());
  }

  // Issue #228
  @Test
  public void createParentDirReturnsFalseWhenRetryAlsoFails() throws IOException {
    File blocker = new File(CoreTestConstants.OUTPUT_DIR_PREFIX + "/fu" + diff + "/blocker2");
    File file = new File(blocker, "logs/testing.txt");
    cleanupList.add(blocker);
    cleanupList.add(blocker.getParentFile());

    blocker.getParentFile().mkdirs();
    assertTrue(blocker.createNewFile());

    // no-op platform (no Android context available)
    AndroidContextUtil platform = new AndroidContextUtil(null);

    assertFalse(FileUtil.createMissingParentDirectories(file, platform));
  }

  // Issue #228: without an explicit AndroidContextUtil, a default one is
  // created to request the app's external storage directories
  @Test
  public void createParentDirWithDefaultPlatformReturnsFalseWhenDirsCannotBeCreated() throws IOException {
    File blocker = tmp.newFile("blocker3");
    File file = new File(blocker, "logs/testing.txt");

    assertFalse(FileUtil.createMissingParentDirectories(file));
    assertFalse(file.getParentFile().exists());
    assertTrue(blocker.isFile());
  }

  @Test
  public void prefixRelativePathPrependsPrefixToRelativePath() {
    assertEquals("/data/app/logs/app.log", FileUtil.prefixRelativePath("/data/app", "logs/app.log"));
  }

  @Test
  public void prefixRelativePathKeepsAbsolutePath() {
    String absolutePath = new File("logs/app.log").getAbsolutePath();
    assertEquals(absolutePath, FileUtil.prefixRelativePath("/data/app", absolutePath));
  }

  @Test
  public void prefixRelativePathIgnoresNullPrefix() {
    assertEquals("logs/app.log", FileUtil.prefixRelativePath(null, "logs/app.log"));
  }

  @Test
  public void prefixRelativePathIgnoresBlankPrefix() {
    assertEquals("logs/app.log", FileUtil.prefixRelativePath("  ", "logs/app.log"));
  }

  @Test
  public void copyReportsFailureToOpenDestination() throws IOException {
    File src = tmp.newFile("src.txt");
    String destination = new File(tmp.getRoot(), "missing-dir/dest.txt").getPath();

    RolloverFailure failure = assertThrows(RolloverFailure.class, () -> fileUtil.copy(src.getPath(), destination));

    String msg = "Failed to copy [" + src.getPath() + "] to [" + destination + "]";
    assertEquals(msg, failure.getMessage());
    assertFalse(new File(destination).exists());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    Status status = statuses.get(0);
    assertEquals(Status.ERROR, status.getLevel());
    assertEquals(msg, status.getMessage());
    assertTrue(status.getThrowable() instanceof FileNotFoundException);
  }

  @Test
  public void copyReportsReadFailureAndClosesBothStreams() throws IOException {
    File src = tmp.newFile("src.txt");
    File destination = new File(tmp.getRoot(), "dest.txt");
    final IOException readFailure = new IOException("read failed");
    final List<Closeable> fileStreams = new ArrayList<Closeable>();

    RolloverFailure failure;
    try (MockedConstruction<BufferedInputStream> inputs = mockConstruction(BufferedInputStream.class,
            (mock, ctx) -> {
              fileStreams.add((Closeable) ctx.arguments().get(0));
              when(mock.read(any(byte[].class))).thenThrow(readFailure);
            })) {
      failure = assertThrows(RolloverFailure.class, () -> fileUtil.copy(src.getPath(), destination.getPath()));

      assertEquals(1, inputs.constructed().size());
      verify(inputs.constructed().get(0)).close();
    } finally {
      for (Closeable c : fileStreams) {
        c.close();
      }
    }

    assertEquals("Failed to copy [" + src.getPath() + "] to [" + destination.getPath() + "]", failure.getMessage());
    // the destination was created, then closed (and so can be deleted)
    assertTrue(destination.isFile());
    assertEquals(0L, destination.length());
    assertTrue(destination.delete());
    assertSame(readFailure, context.getStatusManager().getCopyOfStatusList().get(0).getThrowable());
  }

  @Test
  public void copyReportsReadFailureAndClosesBothStreamsEvenIfClosingFails() throws IOException {
    File src = tmp.newFile("src.txt");
    String destination = new File(tmp.getRoot(), "dest.txt").getPath();
    final IOException readFailure = new IOException("read failed");
    // the streams handed to the (mocked) buffered streams, closed by this test
    final List<Closeable> fileStreams = new ArrayList<Closeable>();

    RolloverFailure failure;
    try (MockedConstruction<BufferedInputStream> inputs = mockConstruction(BufferedInputStream.class,
            (mock, ctx) -> {
              fileStreams.add((Closeable) ctx.arguments().get(0));
              when(mock.read(any(byte[].class))).thenThrow(readFailure);
              doThrow(new IOException("close failed")).when(mock).close();
            });
         MockedConstruction<BufferedOutputStream> outputs = mockConstruction(BufferedOutputStream.class,
            (mock, ctx) -> {
              fileStreams.add((Closeable) ctx.arguments().get(0));
              doThrow(new IOException("close failed")).when(mock).close();
            })) {
      failure = assertThrows(RolloverFailure.class, () -> fileUtil.copy(src.getPath(), destination));

      assertEquals(1, inputs.constructed().size());
      assertEquals(1, outputs.constructed().size());
      verify(inputs.constructed().get(0)).close();
      verify(outputs.constructed().get(0)).close();
    } finally {
      for (Closeable c : fileStreams) {
        c.close();
      }
    }

    String msg = "Failed to copy [" + src.getPath() + "] to [" + destination + "]";
    assertEquals(msg, failure.getMessage());
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(msg, statuses.get(0).getMessage());
    // the close failures don't mask the original one
    assertSame(readFailure, statuses.get(0).getThrowable());
  }
}
