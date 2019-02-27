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

/**
 * A 2-tuple (a double) consisting of a Class and a String. The Class references
 * the hosting class of a component and the String represents the property name
 * under which a nested component is referenced the host.
 * 
 * This class is used by {@link DefaultNestedComponentRegistry}.
 * 
 * @author Ceki Gulcu
 * 
 */
public class HostClassAndPropertyDouble {

  final Class<?> hostClass;
  final String propertyName;

  public HostClassAndPropertyDouble(Class<?> hostClass, String propertyName) {
    this.hostClass = hostClass;
    this.propertyName = propertyName;
  }

  public Class<?> getHostClass() {
    return hostClass;
  }

  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hostClass == null) ? 0 : hostClass.hashCode());
    result = prime * result
        + ((propertyName == null) ? 0 : propertyName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final HostClassAndPropertyDouble other = (HostClassAndPropertyDouble) obj;
    if (hostClass == null) {
      if (other.hostClass != null)
        return false;
    } else if (!hostClass.equals(other.hostClass))
      return false;
    if (propertyName == null) {
      if (other.propertyName != null)
        return false;
    } else if (!propertyName.equals(other.propertyName))
      return false;
    return true;
  }

}
