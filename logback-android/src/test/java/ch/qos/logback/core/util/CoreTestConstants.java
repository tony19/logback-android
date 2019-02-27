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
package ch.qos.logback.core.util;

public class CoreTestConstants {
  /**
   * The reference bogo instructions per second on
   * Ceki's machine (Orion)
   */
  public static long REFERENCE_BIPS = 9000;

  public static final String BASE_DIR = "";

  public static final String TEST_DIR_PREFIX      = BASE_DIR + "src/test/";
  public static final String TEST_INPUT_PREFIX    = TEST_DIR_PREFIX + "input/";
  public static final String JORAN_INPUT_PREFIX   = TEST_INPUT_PREFIX + "joran/";

  public static final String TARGET_DIR           = BASE_DIR + "build/";
  public static final String OUTPUT_DIR_PREFIX    = TARGET_DIR + "test-output/";

  public static final String BASH_PATH_ON_CYGWIN = "c:/cygwin/bin/bash";
  public static final String BASH_PATH_ON_LINUX = "bash";

  public static final String SLOW_JENKINS = "slowJenkins";
}
