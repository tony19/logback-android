package ch.qos.logback.core.rolling.helper;

import java.io.File;
import java.io.FilenameFilter;

public interface FileProvider {
  File[] listFiles(File dir, FilenameFilter filter);
  String[] list(File dir, FilenameFilter filter);
  boolean deleteFile(File file);
  long length(File file);
  boolean exists(File file);
  boolean isDirectory(File file);
}
