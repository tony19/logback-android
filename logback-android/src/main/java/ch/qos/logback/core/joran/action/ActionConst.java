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
package ch.qos.logback.core.joran.action;

/**
 *
 * This class contains costants used by other Actions.
 * 
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public abstract class ActionConst {
  
  public static final String APPENDER_TAG = "appender";
  public static final String REF_ATTRIBUTE = "ref";
  public static final String ADDITIVITY_ATTRIBUTE = "additivity";
  public static final String LEVEL_ATTRIBUTE = "level";
  public static final String CONVERTER_CLASS_ATTRIBUTE = "converterClass";
  public static final String CONVERSION_WORD_ATTRIBUTE = "conversionWord";
  public static final String PATTERN_ATTRIBUTE = "pattern";
  public static final String VALUE_ATTR = "value";
  public static final String ACTION_CLASS_ATTRIBUTE = "actionClass";

  public static final String INHERITED = "INHERITED";
  public static final String NULL = "NULL";
  static final Class<?>[] ONE_STRING_PARAM = new Class[] { String.class };

  public static final String APPENDER_BAG = "APPENDER_BAG";
  //public static final String FILTER_CHAIN_BAG = "FILTER_CHAIN_BAG";
}
