package android.util;

public class Log {
  public static int VERBOSE = 2;
  public static int DEBUG = 3;
  public static int INFO = 4;
  public static int WARN = 5;
  public static int ERROR = 6;
  public static int ASSERT = 7;

  public static int d(String tag, String msg) {
    System.out.println("DEBUG: " + tag + ": " + msg);
    return 0;
  }

  public static int i(String tag, String msg) {
    System.out.println("INFO: " + tag + ": " + msg);
    return 0;
  }

  public static int w(String tag, String msg) {
    System.out.println("WARN: " + tag + ": " + msg);
    return 0;
  }

  public static int e(String tag, String msg) {
    System.out.println("ERROR: " + tag + ": " + msg);
    return 0;
  }
}
