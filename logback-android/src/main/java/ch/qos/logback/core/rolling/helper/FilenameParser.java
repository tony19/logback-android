package ch.qos.logback.core.rolling.helper;

interface FilenameParser<T extends Comparable<T>> {
  T parseFilename(String filename);
}
