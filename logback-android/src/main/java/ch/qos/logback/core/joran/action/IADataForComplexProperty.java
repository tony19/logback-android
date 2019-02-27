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
package ch.qos.logback.core.joran.action;

import ch.qos.logback.core.joran.util.PropertySetter;
import ch.qos.logback.core.util.AggregationType;

/**
 * Lump together several fields for use by {@link NestedComplexPropertyIA}.
 * 
 * @author Ceki
 */
public class IADataForComplexProperty {
  final PropertySetter parentBean;
  final AggregationType aggregationType;
  final String complexPropertyName;
  private Object nestedComplexProperty;
  boolean inError;

  public IADataForComplexProperty(PropertySetter parentBean, AggregationType aggregationType, String complexPropertyName) {
    this.parentBean = parentBean;
    this.aggregationType = aggregationType;
    this.complexPropertyName = complexPropertyName;
  }

  public AggregationType getAggregationType() {
    return aggregationType;
  }

  public Object getNestedComplexProperty() {
    return nestedComplexProperty;
  }

  public String getComplexPropertyName() {
    return complexPropertyName;
  }

  public void setNestedComplexProperty(Object nestedComplexProperty) {
    this.nestedComplexProperty = nestedComplexProperty;
  }
  
  
}
