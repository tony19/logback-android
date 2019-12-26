/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
