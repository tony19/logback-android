package ch.qos.logback.core.rolling.helper;

import java.io.File;
import java.io.FilenameFilter;

public class DefaultFileProvider implements FileProvider {
  public File[] listFiles(File dir, FilenameFilter filter) {
    return dir.listFiles(filter);
  }

  public String[] list(File dir, FilenameFilter filter) {
    return dir.list(filter);
  }

  public boolean deleteFile(File file) {
    return file.delete();
  }

  public long length(File file) {
    return file.length();
  }

  public boolean exists(File file) {
    return file.exists();
  }

  public boolean isDirectory(File file) {
    return file.isDirectory();
  }
}
