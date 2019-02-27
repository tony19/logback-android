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
package ch.qos.logback.core.testUtil;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public class StringListAppender<E> extends AppenderBase<E> {

  Layout<E> layout;
  public List<String> strList = new ArrayList<String>();

  public void start() {
    strList.clear();

    if (layout == null || !layout.isStarted()) {
      return;
    }
    super.start();
  }

  public void stop() {
    super.stop();
  }

  @Override
  protected void append(E eventObject) {
    String res = layout.doLayout(eventObject);
    strList.add(res);
  }

  public Layout<E> getLayout() {
    return layout;
  }

  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }
}
