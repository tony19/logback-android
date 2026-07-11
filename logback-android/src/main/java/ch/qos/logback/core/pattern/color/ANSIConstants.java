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
package ch.qos.logback.core.pattern.color;

/**
 * ANSI escape codes used by the color composite converters (issue #332)
 */
public class ANSIConstants {

  public final static String ESC_START = "\033[";
  public final static String ESC_END = "m";

  public final static String BOLD = "1;";

  public final static String BLACK_FG = "30";
  public final static String RED_FG = "31";
  public final static String GREEN_FG = "32";
  public final static String YELLOW_FG = "33";
  public final static String BLUE_FG = "34";
  public final static String MAGENTA_FG = "35";
  public final static String CYAN_FG = "36";
  public final static String WHITE_FG = "37";
  public final static String DEFAULT_FG = "39";
}
