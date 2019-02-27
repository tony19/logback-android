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
 * Lump together several fields for use by {@link NestedBasicPropertyIA}.
 * 
 * @author Ceki Gulcu
 */
class IADataForBasicProperty {
  final PropertySetter parentBean;
  final AggregationType aggregationType;
  final String propertyName;
  boolean inError;

  IADataForBasicProperty(PropertySetter parentBean, AggregationType aggregationType, String propertyName) {
    this.parentBean = parentBean;
    this.aggregationType = aggregationType;
    this.propertyName = propertyName;
  }
}
