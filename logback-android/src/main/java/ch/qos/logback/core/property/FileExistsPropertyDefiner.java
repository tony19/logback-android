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
package ch.qos.logback.core.property;

import ch.qos.logback.core.PropertyDefinerBase;
import ch.qos.logback.core.util.OptionHelper;

import java.io.File;

/**
 * In conjunction with {@link ch.qos.logback.core.joran.action.PropertyAction} sets
 * the named variable to "true" if the file specified by {@link #setPath(String) path}
 * property exists, to "false" otherwise.
 *
 * @see #getPropertyValue()
 *
 * @author Ceki G&uuml;c&uuml;
 */
public class FileExistsPropertyDefiner extends PropertyDefinerBase {

  String path;

  public String getPath() {
    return path;
  }

  /**
   * Sets the path for the file to search for.
   *
   * @param path the file path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Returns "true" if the file specified by {@link #setPath(String) path} property exists.
   * Returns "false" otherwise.
   *
   * @return "true"|"false" depending on the existence of file
   */
  public String getPropertyValue() {
    if (OptionHelper.isEmpty(path)) {
      addError("The \"path\" property must be set.");
      return null;
    }

    File file = new File(path);
    return booleanAsStr(file.exists());
  }
}
