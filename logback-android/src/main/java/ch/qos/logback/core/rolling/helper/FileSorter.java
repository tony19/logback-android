package ch.qos.logback.core.rolling.helper;

import java.util.Arrays;
import java.util.List;

class FileSorter {

  private final List<FilenameParser> parsers;

  FileSorter(FilenameParser ...parsers) {
    this.parsers = Arrays.asList(parsers);
  }

  @SuppressWarnings("unchecked")
  void sort(String[] filenames) {
    Arrays.sort(filenames, (f1, f2) -> {
      int result = 0;
      for (FilenameParser p : parsers) {
        result += p.parseFilename(f2).compareTo(p.parseFilename(f1));
      }
      return result;
    });
  }

}
