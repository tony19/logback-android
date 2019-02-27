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
package ch.qos.logback.classic;

import ch.qos.logback.core.util.CoreTestConstants;

public class ClassicTestConstants {
  final static public String ISO_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}";
  //pool-1-thread-47
  final static public String NAKED_MAIN_REGEX = ".+";
  final static public String MAIN_REGEX = "\\[" + NAKED_MAIN_REGEX + "\\]";
  final static public String TEST_PREFIX = CoreTestConstants.BASE_DIR + "src/test/";
  final static public String INPUT_PREFIX = TEST_PREFIX + "input/";
  final static public String RESOURCES_PREFIX = TEST_PREFIX + "resources/";
  final static public String JORAN_INPUT_PREFIX = INPUT_PREFIX + "joran/";
  final static public String ISSUES_PREFIX =   ClassicTestConstants.JORAN_INPUT_PREFIX+"issues/";
  final static public String OUTPUT_DIR_PREFIX= CoreTestConstants.OUTPUT_DIR_PREFIX;
}
