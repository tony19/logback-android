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
package ch.qos.logback.core.rolling.helper;

import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.FormatInfo;

/**
 * When asked to convert an integer, <code>IntegerTokenConverter</code> the
 * string value of that integer.
 * 
 * @author Ceki Gulcu
 */
public class IntegerTokenConverter extends DynamicConverter<Object> implements MonoTypedConverter {

  public final static String CONVERTER_KEY = "i";
  
  public String convert(int i) {
    String s = Integer.toString(i);
    FormatInfo formattingInfo = getFormattingInfo();
    if (formattingInfo == null) {
      return s;
    }
    int min = formattingInfo.getMin();
    StringBuilder sbuf = new StringBuilder();
    for (int j = s.length(); j < min; ++j) {
      sbuf.append('0');
    }
    return sbuf.append(s).toString();
  }

  public String convert(Object o) {
    if(o == null) {
      throw new IllegalArgumentException("Null argument forbidden");
    }
    if(o instanceof Integer) {
      Integer i = (Integer) o;
      return convert(i.intValue());
    } 
    throw new IllegalArgumentException("Cannot convert "+o+" of type"+o.getClass().getName());
  }

  public boolean isApplicable(Object o) {
    return (o instanceof Integer);
  }
}
