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
package ch.qos.logback.core.pattern.parser;

import ch.qos.logback.core.pattern.FormatInfo;

public class FormattingNode extends Node {

  FormatInfo formatInfo;

  FormattingNode(int type) {
    super(type);
  }

  FormattingNode(int type, Object value) {
    super(type, value);
  }

  public FormatInfo getFormatInfo() {
    return formatInfo;
  }

  public void setFormatInfo(FormatInfo formatInfo) {
    this.formatInfo = formatInfo;
  }

  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }

    if(!(o instanceof FormattingNode)) {
        return false;
    }
    FormattingNode r = (FormattingNode) o;

    return (formatInfo != null ? formatInfo.equals(r.formatInfo)
        : r.formatInfo == null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (formatInfo != null ? formatInfo.hashCode() : 0);
    return result;
  }
}
