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
 * A {@code MethodDescriptor} describes a particular method that a class
 * supports for external access from other components.
 * 
 * @author Anthony K. Trinh
 */
public class MethodDescriptor {
  private String name;
  private Method method;

  public MethodDescriptor(String name, Method method) {
    this.name = name;
    this.method = method;
  }

  public String getName() {
    return name;
  }

  public Method getMethod() {
    return method;
  }
}
