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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests {@link FileFilterUtil} from within its package (see also
 * ch.qos.logback.core.helpers.FileFilterUtilTest).
 */
public class FileFilterUtilTest {

  private static final String STEM_REGEX = "app\\.(\\d+)\\.log";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void isInstantiable() {
    // only static members, but the class is public and has a public constructor
    assertNotNull(new FileFilterUtil());
  }

  @Test
  public void listsOnlyFilesMatchingTheStemRegex() throws IOException {
    File match = tmpDir.newFile("app.1.log");
    tmpDir.newFile("other.log");

    assertArrayEquals(new File[] { match }, FileFilterUtil.filesInFolderMatchingStemRegex(tmpDir.getRoot(), STEM_REGEX));
  }

  @Test
  public void nullFolderHasNoMatchingFiles() {
    assertEquals(0, FileFilterUtil.filesInFolderMatchingStemRegex(null, STEM_REGEX).length);
  }

  @Test
  public void missingFolderHasNoMatchingFiles() {
    File missing = new File(tmpDir.getRoot(), "missing");

    assertEquals(0, FileFilterUtil.filesInFolderMatchingStemRegex(missing, STEM_REGEX).length);
  }

  @Test
  public void regularFileHasNoMatchingFiles() throws IOException {
    File notAFolder = tmpDir.newFile("app.2.log");

    assertEquals(0, FileFilterUtil.filesInFolderMatchingStemRegex(notAFolder, STEM_REGEX).length);
  }

  @Test
  public void extractsCounterOfMatchingFile() {
    assertEquals(12, FileFilterUtil.extractCounter(new File("logs", "app.12.log"), STEM_REGEX));
  }

  @Test
  public void extractingCounterOfNonMatchingFileFails() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> FileFilterUtil.extractCounter(new File("logs", "other.log"), STEM_REGEX));

    assertEquals("The regex [" + STEM_REGEX + "] should match [other.log]", e.getMessage());
  }

  @Test
  public void extractingCounterRequiresTheWholeNameToMatch() {
    // the stem regex occurs in the name, but does not match all of it
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> FileFilterUtil.extractCounter(new File("logs", "app.12.log.gz"), STEM_REGEX));

    assertEquals("The regex [" + STEM_REGEX + "] should match [app.12.log.gz]", e.getMessage());
  }
}
