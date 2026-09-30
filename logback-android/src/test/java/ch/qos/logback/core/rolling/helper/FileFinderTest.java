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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FileFinderTest {

  public static class FindFiles {
    File[] files;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
      setupTmpDir(tmpDir);
    }

    @Test
    public void findsFilesAcrossMultipleDirs() {
      FileFinder finder = new FileFinder(new DefaultFileProvider());
      String pathPattern = tmpDir.getRoot() + File.separator + FileFinder.regexEscapePath("\\d{4}/\\d{2}/app_\\d{4}\\d{2}\\d{2}.log");
      List<String> actualFiles = finder.findFiles(pathPattern);
      List<String> expectedFileList = new ArrayList<String>();
      for (File f : files) {
        expectedFileList.add(f.getAbsolutePath());
      }
      assertThat(actualFiles, containsInAnyOrder(expectedFileList.toArray(new String[0])));
    }

    private void setupTmpDir(TemporaryFolder tmpDir) throws IOException {
      File[] dirs = new File[] {
        tmpDir.newFolder("2016", "02"),
        tmpDir.newFolder("2017", "12"),
        tmpDir.newFolder("2018", "03"),
        tmpDir.newFolder("2019", "11"),
      };
      files = new File[] {
        tmpDir.newFile("2019/11/app_20191103.log"),
        tmpDir.newFile("2019/11/app_20191102.log"),
        tmpDir.newFile("2019/11/app_20191101.log"),
        tmpDir.newFile("2018/03/app_20180317.log"),
        tmpDir.newFile("2017/12/app_20171225.log"),
        tmpDir.newFile("2016/02/app_20160214.log"),
      };
      for (File f : dirs) {
        f.deleteOnExit();
      }
      for (File f : files) {
        f.deleteOnExit();
      }
    }
  }

  public static class FindAmongOtherEntries {
    File matchingDir;
    File matchingSubDir;
    File matchingFile;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
      matchingSubDir = tmpDir.newFolder("2019", "11");
      matchingDir = matchingSubDir.getParentFile();
      matchingFile = tmpDir.newFile("2019/11/app_20191101.log");
      // a directory not matching its path part, holding a matching file
      tmpDir.newFolder("abcd", "11");
      tmpDir.newFile("abcd/11/app_20191102.log");
      // a regular file matching a directory's path part
      tmpDir.newFile("2020");
    }

    @Test
    public void findsFilesOnlyThroughMatchingDirectories() {
      FileFinder finder = new FileFinder(new DefaultFileProvider());

      List<String> actualFiles = finder.findFiles(pathPattern(tmpDir.getRoot()));

      assertThat(actualFiles, contains(matchingFile.getAbsolutePath()));
    }

    @Test
    public void findsOnlyMatchingDirectories() {
      FileFinder finder = new FileFinder(new DefaultFileProvider());

      List<String> actualDirs = finder.findDirs(pathPattern(tmpDir.getRoot()));

      assertThat(actualDirs, contains(matchingDir.getAbsolutePath(), matchingSubDir.getAbsolutePath()));
    }

    @Test
    public void findsNothingUnderMissingBaseDirectory() {
      FileFinder finder = new FileFinder(new DefaultFileProvider());
      File missingBaseDir = new File(tmpDir.getRoot(), "missing");

      assertThat(finder.findFiles(pathPattern(missingBaseDir)), empty());
      assertThat(finder.findDirs(pathPattern(missingBaseDir)), empty());
    }

    private String pathPattern(File baseDir) {
      return baseDir + File.separator + FileFinder.regexEscapePath("\\d{4}/\\d{2}/app_\\d{8}.log");
    }
  }

  public static class ListPathPart {

    @Test
    public void literalPartListsItsOwnPath() {
      File baseDir = new File("logs").getAbsoluteFile();
      File[] children = { new File(baseDir, "2019") };
      FileProvider fileProvider = mock(FileProvider.class);
      when(fileProvider.listFiles(baseDir, null)).thenReturn(children);

      assertEquals(Arrays.asList(children), new LiteralPathPart("logs").listFiles(fileProvider));
    }

    @Test
    public void regexPartListsWorkingDirectory() {
      File workingDir = new File(".").getAbsoluteFile();
      File[] children = { new File(workingDir, "2019"), new File(workingDir, "notes.txt") };
      FileProvider fileProvider = mock(FileProvider.class);
      when(fileProvider.listFiles(workingDir, null)).thenReturn(children);

      assertEquals(Arrays.asList(children), new RegexPathPart("\\d{4}").listFiles(fileProvider));
      verify(fileProvider).listFiles(workingDir, null);
    }

    @Test
    public void unlistablePathHasNoFiles() {
      FileProvider fileProvider = mock(FileProvider.class);
      when(fileProvider.listFiles(new File("logs").getAbsoluteFile(), null)).thenReturn(null);

      assertTrue(new LiteralPathPart("logs").listFiles(fileProvider).isEmpty());
    }
  }

  public static class SplitPath {
    FileFinder finder;

    @Before
    public void setup() {
      finder = new FileFinder(new DefaultFileProvider());
    }

    @Test
    public void doesNotSplitBaseFilename() {
      assertThat(splitPath("foo.log"), contains("foo.log"));
    }

    @Test
    public void doesNotSplitPathOfLiterals() {
      assertThat(splitPath("/a/b/c.log"), contains("/a/b/c.log"));
    }

    @Test
    public void doesNotSplitPathOfRawRegex() {
      String[] inputs = new String[] {
        "/\\d{4}/\\d{2}/c.log",
        "/logs (.)[x]{1}.+?/\\d{4}/\\d{2}/c.log",
      };
      for (String input : inputs) {
        assertThat(splitPath(input), contains(input));
      }
    }

    @Test
    public void splitsPathOfEscapedRegex() {
      assertThat(splitPath(FileFinder.regexEscapePath("/\\d{4}/\\d{2}/c.log")), contains("", "\\d{4}", "\\d{2}", "c.log"));
      HashMap<String, String[]> inputs = new HashMap<String, String[]>();
      inputs.put("/\\d{4}/\\d{2}/c.log", new String[] { "", "\\d{4}", "\\d{2}", "c.log" });
      inputs.put("/logs (.)[x]{1}.+?/\\d{4}/\\d{2}/c.log", new String[] { "", "logs (.)[x]{1}.+?", "\\d{4}", "\\d{2}", "c.log" });

      for (String key : inputs.keySet()) {
        assertThat(splitPath(FileFinder.regexEscapePath(key)), contains(inputs.get(key)));
      }
    }

    @Test
    public void partMarkedOnlyAsRegexStartIsLiteral() {
      // a regex part needs both the start and the end marker
      String halfMarked = "/logs/(?:\uFFFE)?\\d+/c.log";

      List<PathPart> parts = finder.splitPath(halfMarked);

      assertEquals(1, parts.size());
      assertTrue(parts.get(0) instanceof LiteralPathPart);
      // the marker is removed, the rest is kept verbatim
      assertEquals("/logs/\\d+/c.log", parts.get(0).part.replace(File.separator, "/"));
    }

    private List<String> splitPath(String pattern) {
      List<String> parts = new ArrayList<String>();
      for (PathPart p : finder.splitPath(pattern)) {
        parts.add(p.part);
      }
      return parts;
    }
  }
}
