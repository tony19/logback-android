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
package ch.qos.logback.classic.spi;

import java.io.Serializable;

public class ClassPackagingData implements Serializable {

  private static final long serialVersionUID = -804643281218337001L;
  final String codeLocation;
  final String version;
  private final boolean exact;
  
  public ClassPackagingData(String codeLocation, String version) {
    this.codeLocation = codeLocation;
    this.version = version;
    this.exact = true;
  }

  public ClassPackagingData(String classLocation, String version, boolean exact) {
    this.codeLocation = classLocation;
    this.version = version;
    this.exact = exact;
  }
  
  public String getCodeLocation() {
    return codeLocation;
  }

  public String getVersion() {
    return version;
  }

  public boolean isExact() {
    return exact;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((codeLocation == null) ? 0 : codeLocation.hashCode());
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
    final ClassPackagingData other = (ClassPackagingData) obj;
    if (codeLocation == null) {
      if (other.codeLocation != null)
        return false;
    } else if (!codeLocation.equals(other.codeLocation))
      return false;
    if (exact != other.exact)
      return false;
    if (version == null) {
      if (other.version != null)
        return false;
    } else if (!version.equals(other.version))
      return false;
    return true;
  }
  
}
