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
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;

import java.net.URL;

/**
 * In conjunction with {@link ch.qos.logback.core.joran.action.PropertyAction} sets
 * the named variable to "true" if the {@link #setResource(String) resource} specified
 * by the user is available on the class path, "false" otherwise.
 *
 * @see #getPropertyValue()
 *
 * @author XuHuisheng
 * @author Ceki Gulcu
 * @since 1.1.0
 */
public class ResourceExistsPropertyDefiner extends PropertyDefinerBase {

  String resourceStr;

  public String getResource() {
    return resourceStr;
  }

  /**
   * Sets the resource to search for on the class path.
   *
   * @param resource the resource path
   */
  public void setResource(String resource) {
    this.resourceStr = resource;
  }

  /**
   * Returns the string "true" if the {@link #setResource(String) resource} specified by the
   * user is available on the class path, "false" otherwise.
   *
   * @return "true"|"false" depending on the availability of resource on the classpath
   */
  public String getPropertyValue() {
    if (OptionHelper.isEmpty(resourceStr)) {
      addError("The \"resource\" property must be set.");
      return null;
    }

    URL resourceURL = Loader.getResourceBySelfClassLoader(resourceStr);
    return booleanAsStr(resourceURL != null);
  }

}
