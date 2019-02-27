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
package ch.qos.logback.core.pattern;

abstract public class FormattingConverter<E> extends Converter<E> {

  static final int INITIAL_BUF_SIZE = 256;
  static final int MAX_CAPACITY = 1024;

  
  FormatInfo formattingInfo;

  final public FormatInfo getFormattingInfo() {
    return formattingInfo;
  }

  final public void setFormattingInfo(FormatInfo formattingInfo) {
    if (this.formattingInfo != null) {
      throw new IllegalStateException("FormattingInfo has been already set");
    }
    this.formattingInfo = formattingInfo;
  }

  @Override
  final public void write(StringBuilder buf, E event) {
    String s = convert(event);
    
    if(formattingInfo == null) {
      buf.append(s);
      return;
    }
    
    int min = formattingInfo.getMin();
    int max = formattingInfo.getMax();


    if (s == null) {
      if (0 < min)
        SpacePadder.spacePad(buf, min);
      return;
    }

    int len = s.length();

    if (len > max) {
      if(formattingInfo.isLeftTruncate()) {
        buf.append(s.substring(len - max));
      } else {
        buf.append(s.substring(0, max));
      }
    } else if (len < min) {
      if (formattingInfo.isLeftPad()) {
       SpacePadder.leftPad(buf, s, min);
      } else {
        SpacePadder.rightPad(buf, s, min);
      }
    } else {
      buf.append(s);
    }
  }
}
