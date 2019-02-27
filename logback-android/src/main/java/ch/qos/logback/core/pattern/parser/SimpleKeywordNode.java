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

import java.util.List;

public class SimpleKeywordNode extends FormattingNode {

  List<String> optionList;

  SimpleKeywordNode(Object value) {
    super(Node.SIMPLE_KEYWORD, value);
  }

  protected  SimpleKeywordNode(int type, Object value) {
    super(type, value);
  }

  public List<String> getOptions() {
    return optionList;
  }

  public void setOptions(List<String> optionList) {
    this.optionList = optionList;
  }

  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }

    if (!(o instanceof SimpleKeywordNode)) {
      return false;
    }
    SimpleKeywordNode r = (SimpleKeywordNode) o;

    return (optionList != null ? optionList.equals(r.optionList)
        : r.optionList == null);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    if (optionList == null) {
      buf.append("KeyWord(" + value + "," + formatInfo + ")");
    } else {
      buf.append("KeyWord(" + value + ", " + formatInfo + "," + optionList
          + ")");
    }
    buf.append(printNext());
    return buf.toString();
  }
}
