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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    private List<String> splitPath(String pattern) {
      List<String> parts = new ArrayList<String>();
      for (PathPart p : finder.splitPath(pattern)) {
        parts.add(p.part);
      }
      return parts;
    }
  }
}
