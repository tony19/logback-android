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

import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.EnvUtil;
import ch.qos.logback.core.util.FileUtil;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FileStoreUtilTest {

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void isInstantiable() {
    // only static members, but the class is public and has a public constructor
    assertNotNull(new FileStoreUtil());
  }

  @Test
  public void missingFirstFileIsRejected() throws IOException {
    File missing = new File(tmpDir.getRoot(), "missing");
    File existing = tmpDir.newFile("existing");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FileStoreUtil.areOnSameFileStore(missing, existing));

    assertEquals("File [" + missing + "] does not exist.", e.getMessage());
  }

  @Test
  public void missingSecondFileIsRejected() throws IOException {
    File existing = tmpDir.newFile("existing");
    File missing = new File(tmpDir.getRoot(), "missing");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> FileStoreUtil.areOnSameFileStore(existing, missing));

    assertEquals("File [" + missing + "] does not exist.", e.getMessage());
  }

  @Test
  public void failedFileStoreLookupIsReportedAsRolloverFailure() throws IOException {
    File existing = tmpDir.newFile("existing");
    // passes the existence check, but is gone by the time its store is looked up
    File vanished = new File(tmpDir.getRoot(), "vanished") {
      @Override
      public boolean exists() {
        return true;
      }
    };

    RolloverFailure e = assertThrows(RolloverFailure.class,
        () -> FileStoreUtil.areOnSameFileStore(existing, vanished));

    assertEquals("Failed to check file store equality for [" + existing + "] and [" + vanished + "]", e.getMessage());
    assertNotNull(e.getCause());
  }


  int diff = RandomUtil.getPositiveInt();
  String pathPrefix = CoreTestConstants.OUTPUT_DIR_PREFIX+"fs"+diff+"/";

  @Test
  public void filesOnSameFolderShouldBeOnTheSameFileStore() throws RolloverFailure, IOException {
    if(!EnvUtil.isJDK7OrHigher())
      return;

    File parent = new File(pathPrefix);
    File file = new File(pathPrefix+"filesOnSameFolderShouldBeOnTheSameFileStore");
    FileUtil.createMissingParentDirectories(file);
    file.createNewFile();
    assertTrue(FileStoreUtil.areOnSameFileStore(parent, file));
  }


  // test should be run manually
  @Ignore
  @Test
  public void manual_filesOnDifferentVolumesShouldBeDetectedAsSuch() throws RolloverFailure {
    if(!EnvUtil.isJDK7OrHigher())
      return;

    // author's computer has two volumes
    File c = new File("c:/tmp/");
    File d = new File("d:/");
    assertFalse(FileStoreUtil.areOnSameFileStore(c, d));
  }
}
