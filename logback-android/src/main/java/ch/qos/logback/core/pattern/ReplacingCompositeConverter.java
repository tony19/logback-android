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

import java.util.List;
import java.util.regex.Pattern;

public class ReplacingCompositeConverter<E> extends CompositeConverter<E> {

  Pattern pattern;
  String regex;
  String replacement;

  public void start() {
    final List<String> optionList = getOptionList();
    if (optionList == null) {
      addError("at least two options are expected whereas you have declared none");
      return;
    }

    int numOpts = optionList.size();

    if (numOpts < 2) {
      addError("at least two options are expected whereas you have declared only " + numOpts + "as [" + optionList + "]");
      return;
    }
    regex = optionList.get(0);
    pattern = Pattern.compile(regex);
    replacement = optionList.get(1);
    super.start();
  }

  @Override
  protected String transform(E event, String in) {
    if (!started)
      return in;
    return pattern.matcher(in).replaceAll(replacement);
  }
}