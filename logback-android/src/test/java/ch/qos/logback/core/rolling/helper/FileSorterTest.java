package ch.qos.logback.core.rolling.helper;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class FileSorterTest {

  @Test
  public void sortsDescendingByDate() {

    final String[] FILENAMES = new String[] {
      "/var/logs/my-app/2018-10-31.log",
      "/var/logs/my-app/2019-01-01.log",
      "/var/logs/my-app/1999-03-17.log",
      "/var/logs/my-app/2019-02-14.log",
      "/var/logs/my-app/2016-12-31.log",
      "/var/logs/my-app/2016-12-25.log",
    };
    final String[] EXPECTED_RESULT = new String[] {
      "/var/logs/my-app/2019-02-14.log",
      "/var/logs/my-app/2019-01-01.log",
      "/var/logs/my-app/2018-10-31.log",
      "/var/logs/my-app/2016-12-31.log",
      "/var/logs/my-app/2016-12-25.log",
      "/var/logs/my-app/1999-03-17.log",
    };

    assertThat(sort("/var/logs/my-app/%d{yyyy-MM-dd}.log", FILENAMES), contains(EXPECTED_RESULT));
  }

  @Test
  public void sortsDescendingByDateWithMultipleDatesInPattern() {

    final String[] FILENAMES = new String[] {
      "/var/logs/my-app/2018-10/2018-10-31.log",
      "/var/logs/my-app/2019-01/2019-01-01.log",
      "/var/logs/my-app/1999-03/1999-03-17.log",
      "/var/logs/my-app/2019-02/2019-02-14.log",
      "/var/logs/my-app/2016-12/2016-12-31.log",
      "/var/logs/my-app/2016-12/2016-12-25.log",
    };
    final String[] EXPECTED_RESULT = new String[] {
      "/var/logs/my-app/2019-02/2019-02-14.log",
      "/var/logs/my-app/2019-01/2019-01-01.log",
      "/var/logs/my-app/2018-10/2018-10-31.log",
      "/var/logs/my-app/2016-12/2016-12-31.log",
      "/var/logs/my-app/2016-12/2016-12-25.log",
      "/var/logs/my-app/1999-03/1999-03-17.log",
    };

    assertThat(sort("/var/logs/my-app/%d{yyyy-MM,aux}/%d{yyyy-MM-dd}.log", FILENAMES), contains(EXPECTED_RESULT));
  }

  @Test
  public void sortsDescendingByDateAndInteger() {

    final String[] FILENAMES = new String[] {
      "/var/logs/my-app/2018-10-31/9.log",
      "/var/logs/my-app/2019-01-01/1.log",
      "/var/logs/my-app/1999-03-17/3.log",
      "/var/logs/my-app/2019-01-01/11.log",
      "/var/logs/my-app/2019-01-01/2.log",
      "/var/logs/my-app/2016-12-25/10.log",
    };
    final String[] EXPECTED_RESULT = new String[] {
      "/var/logs/my-app/2019-01-01/11.log",
      "/var/logs/my-app/2019-01-01/2.log",
      "/var/logs/my-app/2019-01-01/1.log",
      "/var/logs/my-app/2018-10-31/9.log",
      "/var/logs/my-app/2016-12-25/10.log",
      "/var/logs/my-app/1999-03-17/3.log",
    };

    assertThat(sort("/var/logs/my-app/%d{yyyy-MM-dd}/%i.log", FILENAMES), contains(EXPECTED_RESULT));
  }

  @Test
  public void sortsDescendingByDateAndIntegerWithMultipleDatesInPattern() {

    final String[] FILENAMES = new String[] {
      "/var/logs/my-app/2018-10/2018-10-31/9.log",
      "/var/logs/my-app/2019-01/2019-01-01/1.log",
      "/var/logs/my-app/1999-03/1999-03-17/3.log",
      "/var/logs/my-app/2019-01/2019-01-01/11.log",
      "/var/logs/my-app/2019-01/2019-01-01/2.log",
      "/var/logs/my-app/2016-12/2016-12-25/10.log",
    };
    final String[] EXPECTED_RESULT = new String[] {
      "/var/logs/my-app/2019-01/2019-01-01/11.log",
      "/var/logs/my-app/2019-01/2019-01-01/2.log",
      "/var/logs/my-app/2019-01/2019-01-01/1.log",
      "/var/logs/my-app/2018-10/2018-10-31/9.log",
      "/var/logs/my-app/2016-12/2016-12-25/10.log",
      "/var/logs/my-app/1999-03/1999-03-17/3.log",
    };

    assertThat(sort("/var/logs/my-app/%d{yyyy-MM,aux}/%d{yyyy-MM-dd}/%i.log", FILENAMES), contains(EXPECTED_RESULT));
  }

  private List<String> sort(String pattern, String[] filenames) {
    FileNamePattern fileNamePattern = new FileNamePattern(pattern, new LoggerContext());
    FileSorter sorter = new FileSorter(new DateParser(fileNamePattern), new IntParser(fileNamePattern));
    sorter.sort(filenames);
    return Arrays.asList(filenames);
  }
}
