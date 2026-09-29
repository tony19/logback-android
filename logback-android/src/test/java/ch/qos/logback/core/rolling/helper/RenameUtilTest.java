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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.EnvUtil;
import ch.qos.logback.core.util.FileUtil;

/**
 * Tests {@link RenameUtil} from within its package (see also
 * ch.qos.logback.core.rolling.RenameUtilTest).
 */
public class RenameUtilTest {

  private static final String CONTENT = "some log data\n";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private final Context context = new ContextBase();
  private RenameUtil renameUtil;
  private File source;

  @Before
  public void setUp() throws IOException {
    renameUtil = new RenameUtil();
    renameUtil.setContext(context);
    source = tmpDir.newFile("app.log");
    FileOutputStream fos = new FileOutputStream(source);
    try {
      fos.write(CONTENT.getBytes("UTF-8"));
    } finally {
      fos.close();
    }
  }

  @Test
  public void renamingFileToItselfIsSkipped() throws IOException {
    renameUtil.rename(source.getPath(), source.getPath());

    assertTrue(hasStatus(Status.WARN, "Source and target files are the same [" + source.getPath() + "]. Skipping."));
    assertFalse(hasStatusStartingWith(Status.INFO, "Renaming file"));
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void renamingMissingFileFails() {
    File missing = new File(tmpDir.getRoot(), "missing.log");
    File target = new File(tmpDir.getRoot(), "renamed.log");

    RolloverFailure e = assertThrows(RolloverFailure.class,
        () -> renameUtil.rename(missing.getPath(), target.getPath()));

    assertEquals("File [" + missing.getPath() + "] does not exist.", e.getMessage());
    assertFalse(target.exists());
  }

  @Test
  public void renamingIntoDirectoryThatCannotBeCreatedFails() throws IOException {
    // the target's grandparent is a regular file, so its parent can't be created
    File blocker = tmpDir.newFile("blocker");
    File target = new File(new File(blocker, "sub"), "renamed.log");

    RolloverFailure e = assertThrows(RolloverFailure.class,
        () -> renameUtil.rename(source.getPath(), target.getPath()));

    assertEquals("Failed to create parent directories for [" + target.getAbsolutePath() + "]", e.getMessage());
    assertEquals(CONTENT, read(source));
  }

  @Test
  public void failedRenameOnSameVolumeKeepsSourceAndWarns() throws IOException {
    // a file cannot replace an existing directory
    File target = tmpDir.newFolder("occupied");

    renameUtil.rename(source.getPath(), target.getPath());

    assertTrue(hasStatus(Status.WARN, "Failed to rename file [" + source + "] as [" + target + "]."));
    assertTrue(hasStatus(Status.WARN, "Please consider leaving the [file] option of RollingFileAppender empty."));
    assertTrue(hasStatus(Status.WARN, "See also " + RenameUtil.RENAMING_ERROR_URL));
    assertFalse(hasStatusStartingWith(Status.WARN, "Detected different file systems"));
    assertEquals(CONTENT, read(source));
    assertTrue(target.isDirectory());
  }

  @Test
  public void failedRenameAcrossVolumesFallsBackToCopying() throws IOException {
    File target = tmpDir.newFolder("occupied");
    RenameUtil spiedRenameUtil = spy(renameUtil);
    doReturn(Boolean.TRUE).when(spiedRenameUtil).areOnDifferentVolumes(any(File.class), any(File.class));
    doNothing().when(spiedRenameUtil).renameByCopying(anyString(), anyString());

    spiedRenameUtil.rename(source.getPath(), target.getPath());

    verify(spiedRenameUtil).renameByCopying(source.getPath(), target.getPath());
    assertTrue(hasStatus(Status.WARN, "Failed to rename file [" + source + "] as [" + target + "]."));
    assertTrue(hasStatus(Status.WARN, "Detected different file systems for source [" + source.getPath()
        + "] and target [" + target.getPath() + "]. Attempting rename by copying."));
    assertFalse(hasStatusStartingWith(Status.WARN, "Please consider"));
  }

  @Test
  public void filesOnSameFileStoreAreNotOnDifferentVolumes() {
    File target = new File(tmpDir.getRoot(), "renamed.log");

    assertEquals(Boolean.FALSE, renameUtil.areOnDifferentVolumes(source, target));
  }

  @Test
  public void filesOnDifferentFileStoresAreOnDifferentVolumes() {
    File target = new File(tmpDir.getRoot(), "renamed.log");

    try (MockedStatic<FileStoreUtil> fileStoreUtil = mockStatic(FileStoreUtil.class)) {
      fileStoreUtil.when(() -> FileStoreUtil.areOnSameFileStore(any(File.class), any(File.class))).thenReturn(false);

      assertEquals(Boolean.TRUE, renameUtil.areOnDifferentVolumes(source, target));

      fileStoreUtil.verify(() -> FileStoreUtil.areOnSameFileStore(source, target.getAbsoluteFile().getParentFile()));
    }
  }

  @Test
  public void volumesAreNotComparedBeforeJava7() {
    // a target that would otherwise make the check inconclusive
    File targetWithoutParent = new File(File.separator);

    try (MockedStatic<EnvUtil> envUtil = mockStatic(EnvUtil.class)) {
      envUtil.when(EnvUtil::isJDK7OrHigher).thenReturn(false);

      assertEquals(Boolean.FALSE, renameUtil.areOnDifferentVolumes(source, targetWithoutParent));
    }
    assertEquals(0, context.getStatusManager().getCount());
  }

  @Test
  public void volumesAreUnknownForTargetWithoutParent() {
    File targetWithoutParent = new File(File.separator);

    assertNull(renameUtil.areOnDifferentVolumes(source, targetWithoutParent));

    assertTrue(hasStatus(Status.WARN, "Parent of target file [" + targetWithoutParent + "] is null"));
  }

  @Test
  public void volumesAreUnknownForTargetInMissingDirectory() {
    File target = new File(new File(tmpDir.getRoot(), "missing"), "renamed.log");

    assertNull(renameUtil.areOnDifferentVolumes(source, target));

    assertTrue(hasStatus(Status.WARN, "Parent of target file [" + target + "] does not exist"));
  }

  @Test
  public void volumesAreUnknownWhenFileStoreCheckFails() {
    // passes the existence check, but is gone by the time its store is looked up
    File vanished = new File(tmpDir.getRoot(), "vanished.log") {
      @Override
      public boolean exists() {
        return true;
      }
    };
    File target = new File(tmpDir.getRoot(), "renamed.log");

    assertNull(renameUtil.areOnDifferentVolumes(vanished, target));

    Status warning = findStatus(Status.WARN, "Error while checking file store equality");
    assertTrue(warning.getThrowable() instanceof RolloverFailure);
  }

  @Test
  public void renameByCopyingCopiesContentAndDeletesSource() throws IOException {
    File target = new File(tmpDir.getRoot(), "copy.log");

    renameUtil.renameByCopying(source.getPath(), target.getPath());

    assertEquals(CONTENT, read(target));
    assertFalse(source.exists());
    assertFalse(hasStatusStartingWith(Status.WARN, "Could not delete"));
  }

  @Test
  public void renameByCopyingWarnsWhenSourceCannotBeDeleted() throws IOException {
    // a non-empty directory cannot be deleted
    File undeletable = tmpDir.newFolder("undeletable");
    assertTrue(new File(undeletable, "child").createNewFile());
    File target = new File(tmpDir.getRoot(), "copy.log");

    try (MockedConstruction<FileUtil> fileUtils = mockConstruction(FileUtil.class)) {
      renameUtil.renameByCopying(undeletable.getPath(), target.getPath());

      assertEquals(1, fileUtils.constructed().size());
      verify(fileUtils.constructed().get(0)).copy(undeletable.getPath(), target.getPath());
    }
    assertTrue(hasStatus(Status.WARN, "Could not delete " + undeletable.getPath()));
    assertTrue(undeletable.isDirectory());
  }

  private Status findStatus(int level, String message) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (s.getLevel() == level && message.equals(s.getMessage())) {
        return s;
      }
    }
    throw new AssertionError("no status [" + message + "] at level " + level);
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

  private static String read(File file) throws IOException {
    InputStream in = new FileInputStream(file);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      int n;
      while ((n = in.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
      return out.toString("UTF-8");
    } finally {
      in.close();
    }
  }
}
