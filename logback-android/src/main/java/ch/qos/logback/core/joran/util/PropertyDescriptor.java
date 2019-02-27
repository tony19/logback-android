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
package ch.qos.logback.core.joran.util;

import java.lang.reflect.Method;

/**
 * A {@code PropertyDescriptor} describes a particular property that a class
 * exports via a pair of accessor methods.
 * 
 * @author Anthony K. Trinh
 */
public class PropertyDescriptor {
  private Method writeMethod;
  private Method readMethod;
  private String name;
  private Class<?> type;

  public PropertyDescriptor(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Method getWriteMethod() {
    return writeMethod;
  }

  public void setWriteMethod(Method writeMethod) {
    this.writeMethod = writeMethod;
  }

  public Method getReadMethod() {
    return readMethod;
  }

  public void setReadMethod(Method readMethod) {
    this.readMethod = readMethod;
  }

  public Class<?> getPropertyType() {
    return type;
  }

  public void setPropertyType(Class<?> type) {
    this.type = type;
  }
}
