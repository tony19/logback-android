package ch.qos.logback.core.rolling.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IntParser implements FilenameParser<Integer> {

  private final Pattern pathPattern;

  IntParser(FileNamePattern fileNamePattern) {
    String pathRegexString = fileNamePattern.toRegex(false, true);
    pathRegexString = FileFinder.unescapePath(pathRegexString);
    this.pathPattern = Pattern.compile(pathRegexString);
  }

  public Integer parseFilename(String filename) {
    Integer intValue = -1;

    try {
      intValue = Integer.parseInt(findToken(filename), 10);
    } catch (NumberFormatException e) {
      // ignore
    }

    return intValue;
  }

  private String findToken(String input) {
    Matcher m = this.pathPattern.matcher(input);
    return (m.find() && m.groupCount() >= 1) ? m.group(1) : "";
  }
}
