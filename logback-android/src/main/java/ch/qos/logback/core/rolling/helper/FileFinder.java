package ch.qos.logback.core.rolling.helper;

import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

class FileFinder {

  private static final String REGEX_MARKER_START = "(?:\uFFFE)?";
  private static final String REGEX_MARKER_END = "(?:\uFFFF)?";
  private FileProvider fileProvider;

  FileFinder(FileProvider fileProvider) {
    this.fileProvider = fileProvider;
  }

  List<String> findFiles(String pathPattern) {
    List<PathPart> pathParts = this.splitPath(pathPattern);
    PathPart pathPart = pathParts.get(0);
    List<File> foundFiles = findFiles(pathPart.listFiles(fileProvider), pathParts, 1);
    return toAbsolutePaths(foundFiles);
  }

  List<String> findDirs(String pathPattern) {
    List<PathPart> pathParts = this.splitPath(pathPattern);
    PathPart pathPart = pathParts.get(0);
    List<File> dirs = new ArrayList<File>();
    findDirs(pathPart.listFiles(fileProvider), pathParts, 1, dirs);
    return toAbsolutePaths(dirs);
  }

  private List<String> toAbsolutePaths(List<File> files) {
    List<String> filenames = new ArrayList<String>();
    for (File f : files) {
      filenames.add(f.getAbsolutePath());
    }
    return filenames;
  }

  private List<File> findFiles(List<File> files, List<PathPart> pathParts, int index) {
    List<File> matchedFiles = new ArrayList<File>();

    PathPart pathPart = pathParts.get(index);
    if (index >= pathParts.size() - 1) {
      for (File file : files) {
        if (pathPart.matches(file)) {
          matchedFiles.add(file);
        }
      }
      return matchedFiles;
    }

    for (File file : files) {
      if (fileProvider.isDirectory(file) && pathPart.matches(file)) {
        List<File> filesInDir = findFiles(Arrays.asList(fileProvider.listFiles(file, null)), pathParts, index + 1);
        matchedFiles.addAll(filesInDir);
      }
    }
    return matchedFiles;
  }

  private void findDirs(List<File> files, List<PathPart> pathParts, int index, List<File> dirs) {
    if (index >= pathParts.size() - 1) {
      return;
    }

    PathPart pathPart = pathParts.get(index);

    for (File file : files) {
      if (fileProvider.isDirectory(file) && pathPart.matches(file)) {
        dirs.add(file);
        findDirs(Arrays.asList(fileProvider.listFiles(file, null)), pathParts, index + 1, dirs);
      }
    }
  }

  List<PathPart> splitPath(String pattern) {
    List<PathPart> parts = new ArrayList<PathPart>();
    List<String> literals = new ArrayList<String>();
    for (String p : pattern.split(File.separator)) {
      final boolean isRegex = p.contains(REGEX_MARKER_START) && p.contains(REGEX_MARKER_END);
      p = p.replace(REGEX_MARKER_START, "").replace(REGEX_MARKER_END, "");
      if (isRegex) {
        if (!literals.isEmpty()) {
          parts.add(new LiteralPathPart(TextUtils.join(File.separator, literals)));
          literals.clear();
        }
        parts.add(new RegexPathPart(p));
      } else {
        literals.add(p);
      }
    }
    if (!literals.isEmpty()) {
      parts.add(new LiteralPathPart(TextUtils.join(File.separator, literals)));
    }
    return parts;
  }

  static String regexEscapePath(String path) {
    if (path.contains(File.separator)) {
      String[] parts = path.split(File.separator);
      for (int i = 0; i < parts.length; i++) {
        if (parts[i].length() > 0) {
          parts[i] = REGEX_MARKER_START + parts[i] + REGEX_MARKER_END;
        }
      }
      return TextUtils.join(File.separator, parts);
    } else {
      return REGEX_MARKER_START + path + REGEX_MARKER_END;
    }
  }

  static String unescapePath(String path) {
    return path.replace(REGEX_MARKER_START, "").replace(REGEX_MARKER_END, "");
  }
}

abstract class PathPart {
  String part;

  PathPart(String part) {
    this.part = part;
  }

  abstract boolean matches(File file);
  abstract List<File> listFiles(FileProvider fileProvider);

  List<File> listFiles(FileProvider fileProvider, String part) {
    File[] files = fileProvider.listFiles(new File(part).getAbsoluteFile(), null);
    if (files == null) {
      files = new File[0];
    }
    return Arrays.asList(files);
  }
}

class LiteralPathPart extends PathPart {
  LiteralPathPart(String part) {
    super(part);
  }

  boolean matches(File file) {
    return file.getName().equals(part);
  }

  List<File> listFiles(FileProvider fileProvider) {
    return listFiles(fileProvider, part);
  }
}

class RegexPathPart extends PathPart {
  private Pattern pattern;

  RegexPathPart(String part) {
    super(part);
    pattern = Pattern.compile(part);
  }

  boolean matches(File file) {
    return pattern.matcher(file.getName()).find();
  }

  List<File> listFiles(FileProvider fileProvider) {
    return listFiles(fileProvider, ".");
  }
}