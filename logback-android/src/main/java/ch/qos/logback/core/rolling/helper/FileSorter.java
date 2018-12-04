package ch.qos.logback.core.rolling.helper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class FileSorter {

  private final List<FilenameParser> parsers;

  FileSorter(FilenameParser ...parsers) {
    this.parsers = Arrays.asList(parsers);
  }

  @SuppressWarnings("unchecked")
  void sort(String[] filenames) {
    Arrays.sort(filenames, new Comparator<String>() {
      @Override
      public int compare(String f1, String f2) {
        int result = 0;

        for (FilenameParser p : parsers) {
          Comparable c2 = p.parseFilename(f2);
          Comparable c1 = p.parseFilename(f1);
          if (c2 != null && c1 != null) {
            result += c2.compareTo(c1);
          }
        }

        // fallback to raw filename comparison
        if (result == 0) {
          result = f2.compareTo(f1);
        }

        return result;
      }
    });
  }

}
