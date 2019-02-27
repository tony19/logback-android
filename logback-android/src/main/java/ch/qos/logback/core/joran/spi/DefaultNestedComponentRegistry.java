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
package ch.qos.logback.core.joran.spi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A registry which maps a property in a host class to a default class.
 * 
 * @author Cek G&uuml;lc&uuml;
 * 
 */
public class DefaultNestedComponentRegistry {

  Map<HostClassAndPropertyDouble, Class<?>> defaultComponentMap = new HashMap<HostClassAndPropertyDouble, Class<?>>();

  public void add(Class<?> hostClass, String propertyName, Class<?> componentClass) {
    HostClassAndPropertyDouble hpDouble = new HostClassAndPropertyDouble(
        hostClass, propertyName.toLowerCase(Locale.US));
    defaultComponentMap.put(hpDouble, componentClass);
  }

  public Class<?> findDefaultComponentType(Class<?> hostClass, String propertyName) {
    propertyName = propertyName.toLowerCase(Locale.US);
    while (hostClass != null) {
      Class<?> componentClass = oneShotFind(hostClass, propertyName);
      if (componentClass != null) {
        return componentClass;
      }
      hostClass = hostClass.getSuperclass();
    }
    return null;
  }

  private Class<?> oneShotFind(Class<?> hostClass, String propertyName) {
    HostClassAndPropertyDouble hpDouble = new HostClassAndPropertyDouble(
        hostClass, propertyName);
    return defaultComponentMap.get(hpDouble);
  }

}
