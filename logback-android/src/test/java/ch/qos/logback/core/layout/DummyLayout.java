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
package ch.qos.logback.core.layout;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

public class DummyLayout<E> extends LayoutBase<E> {

  public static final String DUMMY = "dummy"+CoreConstants.LINE_SEPARATOR;
  String val = DUMMY;
  
  public DummyLayout() {
  }
  
  public DummyLayout(String val) {
    this.val = val;
  }
  
  public String doLayout(E event) {
    return val;
  }

  
}
