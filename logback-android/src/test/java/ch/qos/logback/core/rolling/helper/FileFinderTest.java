package ch.qos.logback.core.rolling.helper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
      String[] actualFiles = finder.findFiles(pathPattern);
      String[] expectedFiles = Stream.of(files).map(File::getAbsolutePath).toArray(String[]::new);
      assertThat(actualFiles, is(arrayContainingInAnyOrder(expectedFiles)));
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
      Stream.concat(Stream.of(dirs), Stream.of(files)).forEach(File::deleteOnExit);
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
      HashMap<String, String[]> inputs = new HashMap<>();
      inputs.put("/\\d{4}/\\d{2}/c.log", new String[] { "", "\\d{4}", "\\d{2}", "c.log" });
      inputs.put("/logs (.)[x]{1}.+?/\\d{4}/\\d{2}/c.log", new String[] { "", "logs (.)[x]{1}.+?", "\\d{4}", "\\d{2}", "c.log" });

      for (String key : inputs.keySet()) {
        assertThat(splitPath(FileFinder.regexEscapePath(key)), contains(inputs.get(key)));
      }
    }

    private List<String> splitPath(String pattern) {
      return finder.splitPath(pattern)
              .stream()
              .map(p -> p.part)
              .collect(Collectors.toList());
    }
  }
}
