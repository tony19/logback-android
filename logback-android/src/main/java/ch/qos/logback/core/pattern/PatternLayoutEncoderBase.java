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

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

public class PatternLayoutEncoderBase<E> extends LayoutWrappingEncoder<E> {

  String pattern;

  // due to popular demand outputPatternAsHeader is set to false by default
  protected boolean outputPatternAsHeader = false;

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public boolean isOutputPatternAsHeader() {
    return outputPatternAsHeader;
  }


  /**
   * Print the pattern string as a header in log files
   *
   * @param outputPatternAsHeader true to enable pattern header
   * @since 1.0.3
   */
  public void setOutputPatternAsHeader(boolean outputPatternAsHeader) {
    this.outputPatternAsHeader = outputPatternAsHeader;
  }


  public boolean isOutputPatternAsPresentationHeader() {
    return outputPatternAsHeader;
  }

  /**
   * @deprecated replaced by {@link #setOutputPatternAsHeader(boolean)}
   * @param outputPatternAsHeader true to enable pattern header
   */
  public void setOutputPatternAsPresentationHeader(boolean outputPatternAsHeader) {
    addWarn("[outputPatternAsPresentationHeader] property is deprecated. Please use [outputPatternAsHeader] option instead.");
    this.outputPatternAsHeader = outputPatternAsHeader;
  }

  @Override
  public void setLayout(Layout<E> layout) {
    throw new UnsupportedOperationException("one cannot set the layout of "
        + this.getClass().getName());
  }

}
